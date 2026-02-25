package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);
    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead createLead(String externalId, String email, String gclid, String fbclid, String fbp, String fbc) {
        log.info("lead.service.create start externalId={} email={}", externalId, email);

        if (leadRepository.existsByExternalId(externalId)) {
            log.warn("lead.service.create duplicate externalId={}", externalId);
            throw new RuntimeException("Lead already exists with this externalId");
        }

        Lead lead = new Lead(externalId, email);
        lead.updateTracking(gclid, fbclid, fbp, fbc);

        Lead savedLead = leadRepository.save(lead);
        log.info("lead.service.create saved leadId={} externalId={} status={}",
                savedLead.getId(), savedLead.getExternalId(), savedLead.getStatus());
        return savedLead;
    }
}
