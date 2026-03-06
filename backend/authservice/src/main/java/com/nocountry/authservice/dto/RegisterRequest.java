package com.nocountry.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        String gclid,
        String fbclid,
        String fbp,
        String fbc,
        String utmSource,
        String utmCampaign
) {
}
