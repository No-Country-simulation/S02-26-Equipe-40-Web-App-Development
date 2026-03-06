package com.nocountry.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    CorsWebFilter corsWebFilter(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}") String allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethods,
            @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Correlation-Id}") String allowedHeaders,
            @Value("${app.cors.exposed-headers:X-Correlation-Id,X-RateLimit-Limit,X-RateLimit-Remaining,X-RateLimit-Window-Seconds,Retry-After}") String exposedHeaders,
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${app.cors.max-age-seconds:3600}") long maxAgeSeconds
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(Arrays.stream(allowedMethods.split(",")).map(String::trim).toList());
        config.setAllowedHeaders(Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList());
        config.setExposedHeaders(Arrays.stream(exposedHeaders.split(",")).map(String::trim).toList());
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
