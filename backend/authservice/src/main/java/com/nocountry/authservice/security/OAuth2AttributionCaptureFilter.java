package com.nocountry.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OAuth2AttributionCaptureFilter extends OncePerRequestFilter {

    private final OAuth2AttributionCookieService attributionCookieService;

    public OAuth2AttributionCaptureFilter(OAuth2AttributionCookieService attributionCookieService) {
        this.attributionCookieService = attributionCookieService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isGoogleAuthorizationStart(request)) {
            attributionCookieService.captureFromRequest(request, response);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isGoogleAuthorizationStart(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/oauth2/authorization/google");
    }
}
