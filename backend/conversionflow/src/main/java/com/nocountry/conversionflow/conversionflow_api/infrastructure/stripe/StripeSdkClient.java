package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

import com.nocountry.conversionflow.conversionflow_api.application.exception.StripeIntegrationException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.StripeWebhookException;
import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeSdkClient implements StripeClient {

    private final StripeProperties stripeProperties;

    public StripeSdkClient(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    @Override
    public StripeCheckoutSession createCheckoutSession(
            String successUrl,
            String cancelUrl,
            String priceId,
            Map<String, String> metadata
    ) {

        try {

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .putAllMetadata(metadata)
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPrice(priceId)
                                            .build()
                            )
                            .build();

            Session session = Session.create(params);
            return new StripeCheckoutSession(session.getId(), session.getUrl());

        } catch (StripeException e) {
            throw new StripeIntegrationException("Error creating Stripe checkout session", e);
        }
    }

    @Override
    public StripeWebhookCheckoutEvent parseCheckoutCompletedEvent(
            String payload,
            String signatureHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
            if (!"checkout.session.completed".equals(event.getType())) {
                return null;
            }

            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("Invalid checkout.session object"));

            String leadIdMetadata = null;
            if (session.getMetadata() != null) {
                leadIdMetadata = session.getMetadata().get("leadId");
            }

            return new StripeWebhookCheckoutEvent(
                    event.getId(),
                    session.getId(),
                    session.getPaymentIntent(),
                    leadIdMetadata
            );
        } catch (Exception e) {
            throw new StripeWebhookException("Error parsing Stripe event", e);
        }
    }

    @Override
    public StripeSessionSnapshot retrieveSession(String checkoutSessionId) {
        try {
            Session session = Session.retrieve(checkoutSessionId);
            String leadIdMetadata = null;
            if (session.getMetadata() != null) {
                leadIdMetadata = session.getMetadata().get("leadId");
            }

            return new StripeSessionSnapshot(
                    session.getId(),
                    session.getPaymentStatus(),
                    session.getPaymentIntent(),
                    leadIdMetadata
            );
        } catch (StripeException e) {
            throw new StripeIntegrationException("Error retrieving Stripe checkout session", e);
        }
    }

    @Override
    public StripePaymentIntentSnapshot retrievePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return new StripePaymentIntentSnapshot(
                    paymentIntent.getId(),
                    paymentIntent.getAmount(),
                    paymentIntent.getCurrency()
            );
        } catch (StripeException e) {
            throw new StripeIntegrationException("Error retrieving Stripe payment intent", e);
        }
    }
}
