package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

public record StripeCheckoutSession(
        String sessionId,
        String url
) {
}
