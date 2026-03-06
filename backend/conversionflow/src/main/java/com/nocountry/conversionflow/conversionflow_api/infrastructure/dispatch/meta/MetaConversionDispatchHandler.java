package com.nocountry.conversionflow.conversionflow_api.infrastructure.dispatch.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.application.port.ConversionDispatchHandler;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.meta.MetaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetaConversionDispatchHandler implements ConversionDispatchHandler {

    private static final Logger log = LoggerFactory.getLogger(MetaConversionDispatchHandler.class);

    private final MetaApiClient metaApiClient;
    private final ObjectMapper objectMapper;

    public MetaConversionDispatchHandler(MetaApiClient metaApiClient, ObjectMapper objectMapper) {
        this.metaApiClient = metaApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Provider provider() {
        return Provider.META;
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
            log.error("meta.payload.parse.error payloadSize={}", payload == null ? 0 : payload.length(), e);
            throw new RuntimeException("Failed to parse payload for Meta conversion", e);
        }
    }

    public void sendConversion(LeadConvertedEvent event) {
        boolean hasMetaId =
                (event.getFbclid() != null && !event.getFbclid().isBlank()) ||
                        (event.getFbp() != null && !event.getFbp().isBlank()) ||
                        (event.getFbc() != null && !event.getFbc().isBlank());

        if (!hasMetaId) {
            log.info("Meta conversion skipped. No fbclid/fbp/fbc for leadId={}", event.getLeadId());
            return;
        }

        try {
            metaApiClient.sendConversion(event);
            log.info("Meta conversion sent. leadId={}, paymentIntentId={}",
                    event.getLeadId(), event.getPaymentIntentId());
        } catch (Exception e) {
            log.error("Error sending conversion to Meta. leadId={}", event.getLeadId(), e);
            throw e;
        }
    }
}
