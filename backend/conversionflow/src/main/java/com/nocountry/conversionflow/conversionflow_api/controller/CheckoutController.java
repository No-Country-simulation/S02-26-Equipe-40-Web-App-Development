package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.StartCheckoutUseCase;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutRequest;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final StartCheckoutUseCase startCheckoutUseCase;

    public CheckoutController(StartCheckoutUseCase startCheckoutUseCase) {
        this.startCheckoutUseCase = startCheckoutUseCase;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> createCheckout(@Valid @RequestBody CheckoutRequest request) {

        log.info("Creating checkout. leadId={}, plan={}", request.getLeadId(), request.getPlan());

        String url = startCheckoutUseCase.execute(
                request.getLeadId(),
                request.getPlan(),
                request.getGclid(),
                request.getFbclid(),
                request.getFbp(),
                request.getFbc(),
                request.getUtmSource(),
                request.getUtmCampaign()
        );

        return ResponseEntity.ok(new CheckoutResponse(url));
    }
}
