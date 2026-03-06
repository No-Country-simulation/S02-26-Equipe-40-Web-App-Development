package com.nocountry.gatewayservice.observability;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;

        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.header(CORRELATION_HEADER, finalCorrelationId))
                .build();
        mutated.getResponse().getHeaders().set(CORRELATION_HEADER, finalCorrelationId);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
