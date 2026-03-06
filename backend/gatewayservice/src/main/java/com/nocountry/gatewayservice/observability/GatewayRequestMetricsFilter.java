package com.nocountry.gatewayservice.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class GatewayRequestMetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    public GatewayRequestMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long durationNanos = System.nanoTime() - startNanos;
                    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String routeId = route != null ? route.getId() : "unmatched";
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    String statusCode = status == null ? "unknown" : String.valueOf(status.value());
                    String method = exchange.getRequest().getMethod() == null
                            ? "unknown"
                            : exchange.getRequest().getMethod().name();

                    Timer.builder("gateway.request.duration")
                            .tag("route", routeId)
                            .tag("status", statusCode)
                            .tag("method", method)
                            .register(meterRegistry)
                            .record(durationNanos, TimeUnit.NANOSECONDS);

                    meterRegistry.counter("gateway.request.count",
                            "route", routeId,
                            "status", statusCode,
                            "method", method).increment();
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
