package com.nocountry.conversionflow.conversionflow_api.service.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleConversionsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleConversionsService.class);

    private final ObjectMapper objectMapper;

    public GoogleConversionsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendConversionFromPayload(String payload) {
        try {
            LeadConvertedEvent event = objectMapper.readValue(payload, LeadConvertedEvent.class);
            sendConversion(event);
        } catch (Exception e) {
            log.error("google.payload.parse.error payloadSize={}", payload == null ? 0 : payload.length(), e);
            throw new RuntimeException("Failed to parse payload for Google conversion", e);
        }
    }

    /**
     * MVP: stub com log.
     * Aqui você conecta o client real do Google Ads (quando for fechar Sprint 6).
     */
    public void sendConversion(LeadConvertedEvent event) {

        if (event.getGclid() == null || event.getGclid().isBlank()) {
            log.info("Google conversion skipped. No gclid. leadId={}", event.getLeadId());
            return;
        }

        log.info("Google conversion (stub) sent. leadId={}, gclid={}, value={}, currency=? timestamp={}",
                event.getLeadId(),
                event.getGclid(),
                event.getConvertedAmount(),
                event.getConvertedAt()
        );
    }
}
