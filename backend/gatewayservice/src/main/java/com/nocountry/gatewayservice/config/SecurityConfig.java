package com.nocountry.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .securityMatcher(ServerWebExchangeMatchers.anyExchange())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .pathMatchers("/health", "/health/**").permitAll()
                        .pathMatchers("/api/v1/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers("/api/v1/webhooks/**", "/api/v1/pixel-events/**").permitAll()
                        .pathMatchers("/api/v1/leads/**", "/api/v1/checkout/**").authenticated()
                        .pathMatchers("/internal/**", "/admin/**", "/actuator/**").hasRole("ADMIN")
                        .anyExchange().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, exception) ->
                                writeError(exchange, 401, "unauthorized", "authentication required"))
                        .accessDeniedHandler((exchange, exception) ->
                                writeError(exchange, 403, "forbidden", "access denied")))
                .build();
    }

    @Bean
    MapReactiveUserDetailsService userDetailsService(
            @Value("${app.security.admin-user:gateway-admin}") String adminUser,
            @Value("${app.security.admin-pass:change-me}") String adminPass,
            PasswordEncoder passwordEncoder
    ) {
        UserDetails admin = User.withUsername(adminUser)
                .password(passwordEncoder.encode(adminPass))
                .roles("ADMIN")
                .build();
        return new MapReactiveUserDetailsService(admin);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience:}") String audience
    ) {
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
        OAuth2TokenValidator<Jwt> validator = buildJwtValidator(issuer, audience);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> buildJwtValidator(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        if (audience == null || audience.isBlank()) {
            return withIssuer;
        }
        Predicate<List<String>> audienceMatches = audiences -> audiences != null && audiences.contains(audience);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<>("aud", audienceMatches);
        return token -> {
            OAuth2TokenValidatorResult issuerResult = withIssuer.validate(token);
            if (issuerResult.hasErrors()) {
                return issuerResult;
            }
            OAuth2TokenValidatorResult audienceResult = audienceValidator.validate(token);
            if (audienceResult.hasErrors()) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "invalid audience", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private Mono<Void> writeError(ServerWebExchange exchange, int status, String error, String message) {
        exchange.getResponse().setRawStatusCode(status);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json");
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String response = toJson(Map.of(
                "error", error,
                "message", message,
                "correlationId", correlationId == null ? "" : correlationId
        ));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String toJson(Map<String, String> payload) {
        return "{\"error\":\"" + escape(payload.get("error")) + "\","
                + "\"message\":\"" + escape(payload.get("message")) + "\","
                + "\"correlationId\":\"" + escape(payload.get("correlationId")) + "\"}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
