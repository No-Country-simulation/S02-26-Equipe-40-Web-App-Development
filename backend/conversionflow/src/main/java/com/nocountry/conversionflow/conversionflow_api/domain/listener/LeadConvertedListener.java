package com.nocountry.conversionflow.conversionflow_api.domain.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class LeadConvertedListener {

    private static final Logger log = LoggerFactory.getLogger(LeadConvertedListener.class);
    private final ConversionDispatchRepository repository;
    private final ObjectMapper objectMapper;

    public LeadConvertedListener(ConversionDispatchRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeadConverted(LeadConvertedEvent event) {
        log.info("lead.converted.event received leadId={} paymentIntentId={}",
                event.getLeadId(), event.getPaymentIntentId());

        String payload = serializeEvent(event);

        createDispatch(event.getLeadId(), Provider.GOOGLE, payload);
        createDispatch(event.getLeadId(), Provider.META, payload);
        createDispatch(event.getLeadId(), Provider.PIPEDRIVE, payload);
    }

    private void createDispatch(Long leadId, Provider provider, String payload) {
        ConversionDispatch dispatch = new ConversionDispatch(leadId, provider, payload);
        ConversionDispatch saved = repository.save(dispatch);
        log.info("dispatch.created dispatchId={} leadId={} provider={} status={}",
                saved.getId(), saved.getLeadId(), saved.getProvider(), saved.getStatus());
    }

    private String serializeEvent(LeadConvertedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LeadConvertedEvent", e);
        }
    }
}
