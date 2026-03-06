package com.nocountry.authservice.config;

import com.nocountry.authservice.security.OAuth2AuthenticationFailureHandler;
import com.nocountry.authservice.security.OAuth2AuthenticationSuccessHandler;
import com.nocountry.authservice.security.OAuth2AttributionCaptureFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;
    private final OAuth2AttributionCaptureFilter attributionCaptureFilter;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler successHandler,
            OAuth2AuthenticationFailureHandler failureHandler,
            OAuth2AttributionCaptureFilter attributionCaptureFilter
    ) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.attributionCaptureFilter = attributionCaptureFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/google/url",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/google/url",
                                "/health",
                                "/health/readiness",
                                "/health/liveness",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .addFilterBefore(attributionCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .build();
    }
}
