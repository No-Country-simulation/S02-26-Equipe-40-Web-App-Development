package com.nocountry.gatewayservice.controller.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/gateway")
public class GatewayInternalController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final boolean rateLimitEnabled;
    private final int rateLimitRequestsPerWindow;
    private final int rateLimitWindowSeconds;

    public GatewayInternalController(
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${app.rate-limit.requests-per-window:60}") int rateLimitRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") int rateLimitWindowSeconds
    ) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimitRequestsPerWindow = rateLimitRequestsPerWindow;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/fallback/{upstream}")
    public ResponseEntity<Map<String, String>> fallback(@PathVariable String upstream) {
        return ResponseEntity.status(503).body(Map.of(
                "error", "gateway_upstream_unavailable",
                "message", "upstream service unavailable",
                "upstream", upstream
        ));
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> diagnostics() {
        Map<String, String> circuits = new LinkedHashMap<>();
        for (var circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            circuits.put(circuitBreaker.getName(), circuitBreaker.getState().name());
        }
        List<String> circuitNames = circuits.keySet().stream().toList();

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "circuitBreakers", circuits,
                "circuitNames", circuitNames,
                "rateLimitEnabled", rateLimitEnabled,
                "rateLimitRequestsPerWindow", rateLimitRequestsPerWindow,
                "rateLimitWindowSeconds", rateLimitWindowSeconds
        ));
    }
}
