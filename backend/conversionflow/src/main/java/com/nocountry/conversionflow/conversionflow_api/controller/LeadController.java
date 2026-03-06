package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.CreateLeadUseCase;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CreateLeadRequestDTO;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leads")
public class LeadController {

    private static final Logger log = LoggerFactory.getLogger(LeadController.class);
    private final CreateLeadUseCase createLeadUseCase;

    public LeadController(CreateLeadUseCase createLeadUseCase) {
        this.createLeadUseCase = createLeadUseCase;
    }

    @PostMapping
    public ResponseEntity<Lead> createLead(@Valid @RequestBody CreateLeadRequestDTO request) {
        log.info("lead.create.request externalId={} email={}", request.externalId(), request.email());

        Lead lead = createLeadUseCase.execute(
                request.externalId(),
                request.email(),
                request.gclid(),
                request.fbclid(),
                request.fbp(),
                request.fbc(),
                request.utmSource(),
                request.utmCampaign()
        );

        log.info("lead.create.success leadId={} externalId={} status={}", lead.getId(), lead.getExternalId(), lead.getStatus());
        return ResponseEntity.ok(lead);
    }
}
