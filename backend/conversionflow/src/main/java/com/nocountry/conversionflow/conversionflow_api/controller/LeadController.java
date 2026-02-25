package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.controller.dto.CreateLeadRequestDTO;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.service.LeadService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leads")
public class LeadController {

    private static final Logger log = LoggerFactory.getLogger(LeadController.class);
    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<Lead> createLead(@Valid @RequestBody CreateLeadRequestDTO request) {
        log.info("lead.create.request externalId={} email={}", request.externalId(), request.email());

        Lead lead = leadService.createLead(
                request.externalId(),
                request.email(),
                request.gclid(),
                request.fbclid(),
                request.fbp(),
                request.fbc()
        );

        log.info("lead.create.success leadId={} externalId={} status={}", lead.getId(), lead.getExternalId(), lead.getStatus());
        return ResponseEntity.ok(lead);
    }
}
