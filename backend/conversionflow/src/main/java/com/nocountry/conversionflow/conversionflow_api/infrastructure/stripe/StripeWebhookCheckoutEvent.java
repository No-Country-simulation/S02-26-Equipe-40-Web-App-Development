package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

public record StripeWebhookCheckoutEvent(
        String eventId,
        String sessionId,
        String paymentIntentId,
        String leadIdMetadata
) {
}
