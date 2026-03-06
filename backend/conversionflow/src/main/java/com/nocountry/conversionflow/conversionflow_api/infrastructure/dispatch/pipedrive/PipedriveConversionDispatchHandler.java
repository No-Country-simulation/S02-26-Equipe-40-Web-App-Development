package com.nocountry.conversionflow.conversionflow_api.infrastructure.dispatch.pipedrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.application.port.ConversionDispatchHandler;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.pipedrive.PipedriveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PipedriveConversionDispatchHandler implements ConversionDispatchHandler {

    private static final Logger log = LoggerFactory.getLogger(PipedriveConversionDispatchHandler.class);
    private final PipedriveClient pipedriveClient;
    private final ObjectMapper objectMapper;

    public PipedriveConversionDispatchHandler(PipedriveClient pipedriveClient, ObjectMapper objectMapper) {
        this.pipedriveClient = pipedriveClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Provider provider() {
        return Provider.PIPEDRIVE;
    }

    @Override
    public void dispatch(String payload) {
        syncFromPayload(payload);
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
