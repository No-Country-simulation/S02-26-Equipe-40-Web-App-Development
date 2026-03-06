package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.ConfirmPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    public StripeWebhookController(ConfirmPaymentUseCase confirmPaymentUseCase) {
        this.confirmPaymentUseCase = confirmPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        log.info("stripe.webhook.request received payloadSize={} signatureHeaderPresent={}",
                payload == null ? 0 : payload.length(),
                sigHeader != null && !sigHeader.isBlank());

        confirmPaymentUseCase.execute(payload, sigHeader);

        log.info("stripe.webhook.request processed");
        return ResponseEntity.ok("ok");
    }
}
