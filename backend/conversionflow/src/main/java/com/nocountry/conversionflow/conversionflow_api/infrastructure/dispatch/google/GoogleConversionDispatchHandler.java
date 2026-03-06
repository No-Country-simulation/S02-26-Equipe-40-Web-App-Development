package com.nocountry.conversionflow.conversionflow_api.infrastructure.dispatch.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.application.port.ConversionDispatchHandler;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleConversionDispatchHandler implements ConversionDispatchHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleConversionDispatchHandler.class);

    private final ObjectMapper objectMapper;

    public GoogleConversionDispatchHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public void dispatch(String payload) {
        sendConversionFromPayload(payload);
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
