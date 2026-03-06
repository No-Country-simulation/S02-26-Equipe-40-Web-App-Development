package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.application.exception.InvalidInputException;
import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.service.LeadConversionService;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeClient;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripePaymentIntentSnapshot;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeWebhookCheckoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class ConfirmPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmPaymentUseCase.class);

    private final StripeProperties stripeProperties;
    private final StripeClient stripeClient;
    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final LeadConversionService leadConversionService;
    private final ApplicationEventPublisher eventPublisher;

    public ConfirmPaymentUseCase(
            StripeProperties stripeProperties,
            StripeClient stripeClient,
            LeadRepository leadRepository,
            PaymentRepository paymentRepository,
            LeadConversionService leadConversionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.stripeProperties = stripeProperties;
        this.stripeClient = stripeClient;
        this.leadRepository = leadRepository;
        this.paymentRepository = paymentRepository;
        this.leadConversionService = leadConversionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(String payload, String signatureHeader) {
        log.info("usecase.confirmPayment.start payloadSize={}", payload == null ? 0 : payload.length());

        StripeWebhookCheckoutEvent event = stripeClient.parseCheckoutCompletedEvent(payload, signatureHeader);
        if (event == null) {
            log.info("usecase.confirmPayment.ignored nonCheckoutCompletedEvent=true");
            return;
        }

        log.info("usecase.confirmPayment.eventParsed eventId={} sessionId={}", event.eventId(), event.sessionId());
        String leadIdStr = blankToNull(event.leadIdMetadata());
        if (leadIdStr == null) {
            log.warn("usecase.confirmPayment.missingLeadIdMetadata sessionId={}", event.sessionId());
            return;
        }

        Long leadId;
        try {
            leadId = Long.valueOf(leadIdStr);
        } catch (Exception e) {
            log.warn("usecase.confirmPayment.invalidLeadIdMetadata leadId={} sessionId={}", leadIdStr, event.sessionId());
            return;
        }

        Lead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            log.warn("usecase.confirmPayment.leadNotFound leadId={}", leadId);
            return;
        }

        String stripeEventId = event.eventId();
        String stripeSessionId = event.sessionId();
        String paymentIntentId = blankToNull(event.paymentIntentId());

        if (stripeEventId != null && paymentRepository.existsByStripeEventId(stripeEventId)) {
            log.info("usecase.confirmPayment.duplicateEvent stripeEventId={}", stripeEventId);
            return;
        }
        if (paymentIntentId != null && paymentRepository.existsByTransactionId(paymentIntentId)) {
            log.info("usecase.confirmPayment.duplicatePaymentIntent paymentIntentId={}", paymentIntentId);
            return;
        }

        StripePaymentIntentSnapshot paymentIntent = retrievePaymentIntent(paymentIntentId);

        long amountCents = paymentIntent.amountCents() == null ? 0L : paymentIntent.amountCents();
        String currency = paymentIntent.currency() == null ? stripeProperties.getCurrency() : paymentIntent.currency();
        BigDecimal amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));

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

        boolean convertedNow = leadConversionService.markAsWon(lead, amount);
        if (convertedNow) {
            leadRepository.save(lead);
            log.info("usecase.confirmPayment.leadUpdated leadId={} status={} convertedAmount={}",
                    lead.getId(), lead.getStatus(), lead.getConvertedAmount());
            publishLeadConvertedEvent(lead, paymentIntentId);
        }

        log.info("usecase.confirmPayment.success leadId={} paymentIntentId={}", lead.getId(), paymentIntentId);
    }

    private StripePaymentIntentSnapshot retrievePaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new InvalidInputException("Missing paymentIntentId in checkout session");
        }
        return stripeClient.retrievePaymentIntent(paymentIntentId);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
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

        eventPublisher.publishEvent(event);
    }
}
