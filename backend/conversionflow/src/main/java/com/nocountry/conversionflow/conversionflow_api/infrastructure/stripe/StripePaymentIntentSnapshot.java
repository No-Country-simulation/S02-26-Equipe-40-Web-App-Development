package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

public record StripePaymentIntentSnapshot(
        String paymentIntentId,
        Long amountCents,
        String currency
) {
}
