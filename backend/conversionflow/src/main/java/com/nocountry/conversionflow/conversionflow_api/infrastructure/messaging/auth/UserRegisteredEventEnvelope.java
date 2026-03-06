package com.nocountry.conversionflow.conversionflow_api.infrastructure.messaging.auth;

public record UserRegisteredEventEnvelope(
        String eventId,
        String eventVersion,
        String eventName,
        String occurredAt,
        String correlationId,
        String idempotencyKey,
        Payload payload
) {
    public record Payload(
            String authUserId,
            String email,
            String provider,
            Attribution attribution
    ) {
    }

    public record Attribution(
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
    }
}
