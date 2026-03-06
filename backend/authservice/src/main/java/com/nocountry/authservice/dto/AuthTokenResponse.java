package com.nocountry.authservice.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String userId,
        String email
) {
}
