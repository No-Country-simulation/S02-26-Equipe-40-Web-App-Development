package com.nocountry.conversionflow.conversionflow_api.service.pipedrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.pipedrive.PipedriveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PipedriveService {

    private static final Logger log = LoggerFactory.getLogger(PipedriveService.class);
    private final PipedriveClient pipedriveClient;
    private final ObjectMapper objectMapper;

    public PipedriveService(PipedriveClient pipedriveClient, ObjectMapper objectMapper) {
        this.pipedriveClient = pipedriveClient;
        this.objectMapper = objectMapper;
    }

    public void syncFromPayload(String payload) {
        try {
            LeadConvertedEvent event = objectMapper.readValue(payload, LeadConvertedEvent.class);
            syncConvertedLead(event);
        } catch (Exception e) {
            log.error("pipedrive.payload.parse.error payloadSize={}", payload == null ? 0 : payload.length(), e);
            throw new RuntimeException("Failed to parse payload for Pipedrive sync", e);
        }
    }

    public void syncConvertedLead(LeadConvertedEvent event) {
        log.info("pipedrive.sync.start leadId={} paymentIntentId={}", event.getLeadId(), event.getPaymentIntentId());
        pipedriveClient.syncConvertedLead(event);
        log.info("pipedrive.sync.success leadId={} paymentIntentId={}", event.getLeadId(), event.getPaymentIntentId());
    }
}
