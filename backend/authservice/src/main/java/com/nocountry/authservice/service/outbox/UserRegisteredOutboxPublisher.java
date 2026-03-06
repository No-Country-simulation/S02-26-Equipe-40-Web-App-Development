package com.nocountry.authservice.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.authservice.domain.User;
import com.nocountry.authservice.domain.outbox.OutboxEvent;
import com.nocountry.authservice.repository.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserRegisteredOutboxPublisher {

    private static final String EVENT_TYPE = "UserRegistered";
    private static final String AGGREGATE_TYPE = "User";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public UserRegisteredOutboxPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(User user, UserRegisteredEventPayload.Attribution attribution) {
        String idempotencyKey = EVENT_TYPE + ":" + user.getId();
        if (outboxEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        UserRegisteredEventPayload payload = new UserRegisteredEventPayload(
                user.getId().toString(),
                user.getEmail(),
                user.getProvider().name(),
                attribution
        );
        UserRegisteredEventEnvelope envelope = new UserRegisteredEventEnvelope(
                UUID.randomUUID().toString(),
                "1",
                EVENT_TYPE,
                OffsetDateTime.now().toString(),
                null,
                idempotencyKey,
                payload
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventType(EVENT_TYPE);
        event.setAggregateType(AGGREGATE_TYPE);
        event.setAggregateId(user.getId().toString());
        event.setIdempotencyKey(idempotencyKey);
        event.setPayloadJson(toJson(envelope));

        outboxEventRepository.save(event);
    }

    private String toJson(UserRegisteredEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("user_registered_payload_serialization_failed", exception);
        }
    }
}
