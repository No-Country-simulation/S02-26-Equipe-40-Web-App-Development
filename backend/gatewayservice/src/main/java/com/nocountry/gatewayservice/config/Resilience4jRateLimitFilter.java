package com.nocountry.gatewayservice.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Resilience4jRateLimitFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final int requestsPerWindow;
    private final int windowSeconds;
    private final RateLimiterConfig baseConfig;
    private final Map<String, RateLimiter> limitersByKey = new ConcurrentHashMap<>();

    public Resilience4jRateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.requests-per-window:60}") int requestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${app.rate-limit.timeout-ms:0}") long timeoutMs
    ) {
        this.enabled = enabled;
        this.requestsPerWindow = requestsPerWindow;
        this.windowSeconds = windowSeconds;
        this.baseConfig = RateLimiterConfig.custom()
                .limitForPeriod(requestsPerWindow)
                .limitRefreshPeriod(Duration.ofSeconds(windowSeconds))
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled || isExcludedPath(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        String key = resolveClientKey(exchange.getRequest());
        RateLimiter limiter = limitersByKey.computeIfAbsent(key, this::newRateLimiter);
        if (!limiter.acquirePermission()) {
            return writeTooManyRequests(exchange.getResponse(), limiter);
        }

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(requestsPerWindow));
        response.getHeaders().set("X-RateLimit-Remaining", String.valueOf(limiter.getMetrics().getAvailablePermissions()));
        response.getHeaders().set("X-RateLimit-Window-Seconds", String.valueOf(windowSeconds));
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -15;
    }

    private RateLimiter newRateLimiter(String key) {
        return RateLimiter.of("gatewayClient:" + sanitize(key), baseConfig);
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/health")
                || path.startsWith("/actuator")
                || path.startsWith("/internal")
                || path.startsWith("/admin")
                || path.startsWith("/oauth2")
                || path.startsWith("/login/oauth2")
                || path.startsWith("/api/v1/webhooks");
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "token:" + authorization.substring(7).trim();
        }

        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return "ip:" + remoteAddress.getAddress().getHostAddress();
        }

        return "ip:unknown";
    }

    private Mono<Void> writeTooManyRequests(ServerHttpResponse response, RateLimiter limiter) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set("Content-Type", "application/json");
        response.getHeaders().set("Retry-After", String.valueOf(windowSeconds));
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(requestsPerWindow));
        response.getHeaders().set("X-RateLimit-Remaining", String.valueOf(limiter.getMetrics().getAvailablePermissions()));
        response.getHeaders().set("X-RateLimit-Window-Seconds", String.valueOf(windowSeconds));
        byte[] body = "{\"error\":\"rate_limit_exceeded\",\"message\":\"too many requests\"}".getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._:-]", "_");
    }
}
