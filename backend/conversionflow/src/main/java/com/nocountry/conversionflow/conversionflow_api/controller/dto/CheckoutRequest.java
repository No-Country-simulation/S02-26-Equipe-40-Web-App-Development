package com.nocountry.conversionflow.conversionflow_api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {
    @NotNull
    private Long leadId;

    @NotBlank
    private String plan;

    private String gclid;
    private String fbclid;
    private String fbp;
    private String fbc;
    private String utmSource;
    private String utmCampaign;
}
