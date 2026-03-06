package com.nocountry.authservice.controller;

import com.nocountry.authservice.config.ApiPaths;
import com.nocountry.authservice.dto.AuthTokenResponse;
import com.nocountry.authservice.dto.LoginRequest;
import com.nocountry.authservice.dto.RegisterRequest;
import com.nocountry.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping({ApiPaths.PUBLIC_AUTH_V1, ApiPaths.LEGACY_AUTH})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google/url")
    public ResponseEntity<Map<String, String>> googleAuthorizationUrl(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/oauth2/authorization/google");
        copyQueryParamIfPresent(request, builder, "gclid");
        copyQueryParamIfPresent(request, builder, "fbclid");
        copyQueryParamIfPresent(request, builder, "fbp");
        copyQueryParamIfPresent(request, builder, "fbc");
        copyQueryParamIfPresent(request, builder, "utm_source");
        copyQueryParamIfPresent(request, builder, "utm_campaign");
        return ResponseEntity.ok(Map.of("url", builder.build().toUriString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    private void copyQueryParamIfPresent(HttpServletRequest request, UriComponentsBuilder builder, String key) {
        String value = request.getParameter(key);
        if (value != null && !value.isBlank()) {
            builder.queryParam(key, value.trim());
        }
    }

}
