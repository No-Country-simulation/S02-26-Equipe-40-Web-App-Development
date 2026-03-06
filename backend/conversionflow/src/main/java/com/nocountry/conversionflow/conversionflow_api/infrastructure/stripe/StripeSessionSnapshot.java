package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

public record StripeSessionSnapshot(
        String sessionId,
        String paymentStatus,
        String paymentIntentId,
        String leadIdMetadata
) {
}
