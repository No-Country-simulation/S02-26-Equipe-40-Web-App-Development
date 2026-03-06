package com.nocountry.conversionflow.conversionflow_api.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateLeadRequestDTO(

        @NotBlank(message = "ExternalId is required")
        String externalId,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String gclid,
        String fbclid,
        String fbp,
        String fbc,
        String utmSource,
        String utmCampaign

) {}
