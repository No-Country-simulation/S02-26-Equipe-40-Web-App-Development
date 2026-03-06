package com.nocountry.conversionflow.conversionflow_api.controller.dto;

public record PixelEventPurchaseRequest(
        Long leadId,
        String externalId,
        String checkoutSessionId,
        String gclid,
        String fbclid,
        String fbp,
        String fbc,
        String utmSource,
        String utmCampaign
) {
}
