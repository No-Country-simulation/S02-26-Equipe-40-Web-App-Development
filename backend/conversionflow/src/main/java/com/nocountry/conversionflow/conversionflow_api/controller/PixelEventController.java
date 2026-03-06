package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.CapturePixelEventUseCase;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.PixelEventPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pixel-events")
public class PixelEventController {

    private static final Logger log = LoggerFactory.getLogger(PixelEventController.class);

    private final CapturePixelEventUseCase capturePixelEventUseCase;

    public PixelEventController(CapturePixelEventUseCase capturePixelEventUseCase) {
        this.capturePixelEventUseCase = capturePixelEventUseCase;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Void> capturePurchase(@RequestBody PixelEventPurchaseRequest request) {
        log.info("pixel.purchase.request leadId={} externalId={}", request.leadId(), request.externalId());

        capturePixelEventUseCase.execute(
                request.leadId(),
                request.externalId(),
                request.checkoutSessionId(),
                request.gclid(),
                request.fbclid(),
                request.fbp(),
                request.fbc(),
                request.utmSource(),
                request.utmCampaign()
        );

        return ResponseEntity.accepted().build();
    }
}
