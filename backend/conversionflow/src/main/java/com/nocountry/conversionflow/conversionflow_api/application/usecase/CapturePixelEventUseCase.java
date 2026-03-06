package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.application.exception.InvalidInputException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.LeadNotFoundException;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.service.LeadConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class CapturePixelEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(CapturePixelEventUseCase.class);

    private final LeadRepository leadRepository;
    private final LeadConversionService leadConversionService;
    private final ApplicationEventPublisher eventPublisher;

    public CapturePixelEventUseCase(
            LeadRepository leadRepository,
            LeadConversionService leadConversionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.leadRepository = leadRepository;
        this.leadConversionService = leadConversionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(
            Long leadId,
            String externalId,
            String checkoutSessionId,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
        if (leadId == null && (externalId == null || externalId.isBlank())) {
            throw new InvalidInputException("leadId or externalId is required");
        }

        Lead lead = resolveLead(leadId, externalId);
        lead.updateTracking(gclid, fbclid, fbp, fbc, utmSource, utmCampaign);
        boolean convertedNow = leadConversionService.markAsWon(lead, BigDecimal.ZERO);
        leadRepository.save(lead);

        if (convertedNow) {
            String fallbackEventId = buildFallbackEventId(lead.getId(), checkoutSessionId);
            publishLeadConvertedEvent(lead, fallbackEventId);
            log.info("usecase.pixelEvent.capture.markedWonByFallback leadId={} fallbackEventId={}",
                    lead.getId(), fallbackEventId);
        }

        log.info("usecase.pixelEvent.capture.success leadId={} externalId={} checkoutSessionIdProvided={} gclid={} fbclid={} fbp={} fbc={} utmSource={} utmCampaign={}",
                lead.getId(),
                lead.getExternalId(),
                blankToNull(checkoutSessionId) != null,
                blankToNull(gclid) != null,
                blankToNull(fbclid) != null,
                blankToNull(fbp) != null,
                blankToNull(fbc) != null,
                blankToNull(utmSource) != null,
                blankToNull(utmCampaign) != null);
    }

    private Lead resolveLead(Long leadId, String externalId) {
        if (leadId != null) {
            return leadRepository.findById(leadId)
                    .orElseThrow(() -> new LeadNotFoundException("Lead not found: " + leadId));
        }

        return leadRepository.findByExternalId(externalId)
                .orElseThrow(() -> new LeadNotFoundException("Lead not found for externalId: " + externalId));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String buildFallbackEventId(Long leadId, String checkoutSessionId) {
        String normalizedSessionId = blankToNull(checkoutSessionId);
        if (normalizedSessionId != null) {
            return "pixel-fallback:" + normalizedSessionId;
        }
        return "pixel-fallback:lead-" + leadId;
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
