package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

import java.util.Map;

public interface StripeClient {

    StripeCheckoutSession createCheckoutSession(
            String successUrl,
            String cancelUrl,
            String priceId,
            Map<String, String> metadata
    );

    StripeWebhookCheckoutEvent parseCheckoutCompletedEvent(
            String payload,
            String signatureHeader
    );

    StripeSessionSnapshot retrieveSession(String checkoutSessionId);

    StripePaymentIntentSnapshot retrievePaymentIntent(String paymentIntentId);
}
