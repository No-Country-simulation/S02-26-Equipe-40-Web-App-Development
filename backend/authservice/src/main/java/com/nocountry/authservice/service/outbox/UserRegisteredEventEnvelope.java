package com.nocountry.authservice.service.outbox;

public record UserRegisteredEventEnvelope(
        String eventId,
        String eventVersion,
        String eventName,
        String occurredAt,
        String correlationId,
        String idempotencyKey,
        UserRegisteredEventPayload payload
) {
}
