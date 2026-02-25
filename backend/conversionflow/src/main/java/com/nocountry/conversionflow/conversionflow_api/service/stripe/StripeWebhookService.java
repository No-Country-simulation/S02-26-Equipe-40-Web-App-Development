package com.nocountry.conversionflow.conversionflow_api.service.stripe;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            LeadRepository leadRepository,
            PaymentRepository paymentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================
    // CHECKOUT (consolidado aqui)
    // =========================================================

    @Transactional
    public String createCheckoutSession(
            Long leadId,
            String plan,
            String gclid,
            String fbclid,
            String fbp,
            String fbc
    ) throws StripeException {
        log.info("stripe.checkout.create start leadId={} plan={}", leadId, plan);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        // Atualiza tracking e marca início do checkout
        lead.updateTracking(gclid, fbclid, fbp, fbc);
        lead.startCheckout();
        leadRepository.save(lead);

        String normalizedPlan = plan.trim().toLowerCase(Locale.ROOT);
        String priceId = resolvePriceId(normalizedPlan);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl())
                .setCancelUrl(stripeProperties.getCancelUrl())
                .putMetadata("leadId", String.valueOf(lead.getId()))
                .putMetadata("plan", normalizedPlan)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .build();

        Session session = Session.create(params);

        if (session.getUrl() == null || session.getUrl().isBlank()) {
            throw new IllegalStateException("Stripe did not return checkout URL");
        }

        log.info("stripe.checkout.create success leadId={} plan={} sessionId={}",
                leadId, normalizedPlan, session.getId());
        return session.getUrl();
    }

    private String resolvePriceId(String normalizedPlan) {
        Map<String, String> prices = stripeProperties.getPrices();
        if (prices == null || prices.isEmpty()) {
            throw new IllegalStateException("stripe.prices is not configured");
        }

        String priceId = prices.get(normalizedPlan);
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException("No Stripe priceId configured for plan: " + normalizedPlan);
        }

        return priceId;
    }

    // =========================================================
    // WEBHOOK (source of truth)
    // =========================================================

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        log.info("stripe.webhook.handle start payloadSize={}", payload == null ? 0 : payload.length());

        Event event = constructStripeEvent(payload, signatureHeader);
        log.info("stripe.webhook.handle parsed eventType={} eventId={}", event.getType(), event.getId());

        // MVP: só tratamos checkout.session.completed
        if (!"checkout.session.completed".equals(event.getType())) {
            log.info("stripe.webhook.handle ignored eventType={} eventId={}", event.getType(), event.getId());
            return;
        }

        Session session = extractSession(event);

        String leadIdStr = getMetadata(session, "leadId");
        if (leadIdStr == null) {
            log.warn("Stripe session without leadId metadata. sessionId={}", session.getId());
            return;
        }

        Long leadId;
        try {
            leadId = Long.valueOf(leadIdStr);
        } catch (Exception e) {
            log.warn("Invalid leadId metadata. leadId={}, sessionId={}", leadIdStr, session.getId());
            return;
        }

        Lead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            log.warn("Lead not found for leadId={}", leadId);
            return;
        }

        String stripeEventId = event.getId();
        String stripeSessionId = session.getId();
        String paymentIntentId = session.getPaymentIntent();

        // Idempotência forte: se já processamos este evento ou este paymentIntent, ignora
        if (stripeEventId != null && paymentRepository.existsByStripeEventId(stripeEventId)) {
            log.info("Webhook already processed (stripeEventId={})", stripeEventId);
            return;
        }
        if (paymentIntentId != null && paymentRepository.existsByTransactionId(paymentIntentId)) {
            log.info("Payment already processed (paymentIntentId={})", paymentIntentId);
            return;
        }

        // Obtém valor/currency via PaymentIntent (mais confiável)
        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);

        long amountCents = paymentIntent.getAmount() == null ? 0L : paymentIntent.getAmount();
        String currency = paymentIntent.getCurrency() == null ? stripeProperties.getCurrency() : paymentIntent.getCurrency();

        BigDecimal amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));

        // Salva Payment (fonte da verdade)
        Payment payment = new Payment();
        payment.setStripeEventId(stripeEventId);
        payment.setStripeSessionId(stripeSessionId);
        payment.setTransactionId(paymentIntentId);
        payment.setAmountCents(amountCents);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PAID);
        payment.setLead(lead);
        payment.setCreatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
        log.info("stripe.webhook.payment.saved leadId={} stripeEventId={} paymentIntentId={} amountCents={} currency={}",
                lead.getId(), stripeEventId, paymentIntentId, amountCents, currency);

        // Atualiza Lead como WON (idempotente pelo Payment)
        if (!lead.isWon()) {
            lead.markAsWon(amount);
            leadRepository.save(lead);
            log.info("stripe.webhook.lead.updated leadId={} status={} convertedAmount={}",
                    lead.getId(), lead.getStatus(), lead.getConvertedAmount());
        } else {
            log.info("stripe.webhook.lead.alreadyWon leadId={}", lead.getId());
        }

        // Publica evento AFTER_COMMIT (listener vai criar dispatches)
        publishLeadConvertedEvent(lead, paymentIntentId);
    }

    private Event constructStripeEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("stripe.webhook.signature.invalid", e);
            throw new RuntimeException("Invalid Stripe signature", e);
        } catch (Exception e) {
            log.error("stripe.webhook.parse.error", e);
            throw new RuntimeException("Error parsing Stripe event", e);
        }
    }

    private Session extractSession(Event event) {
        return (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Invalid session object"));
    }

    private String getMetadata(Session session, String key) {
        if (session.getMetadata() == null) return null;
        String value = session.getMetadata().get(key);
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalStateException("Missing paymentIntentId in checkout session");
        }
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("stripe.paymentIntent.retrieve.error paymentIntentId={}", paymentIntentId, e);
            throw new RuntimeException("Error retrieving PaymentIntent", e);
        }
    }

    private void publishLeadConvertedEvent(Lead lead, String paymentIntentId) {
        LeadConvertedEvent event = new LeadConvertedEvent(
                lead.getId(),
                lead.getExternalId(),
                lead.getEmail(),
                lead.getGclid(),
                lead.getFbclid(),
                lead.getFbp(),
                lead.getFbc(),
                paymentIntentId,
                lead.getConvertedAmount(),
                lead.getConvertedAt()
        );

        log.info("Publishing LeadConvertedEvent leadId={}, paymentIntentId={}", lead.getId(), paymentIntentId);
        eventPublisher.publishEvent(event);
    }
}
