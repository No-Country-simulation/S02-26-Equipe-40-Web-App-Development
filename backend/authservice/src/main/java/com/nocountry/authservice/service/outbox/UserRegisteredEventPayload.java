package com.nocountry.authservice.service.outbox;

public record UserRegisteredEventPayload(
        String authUserId,
        String email,
        String provider,
        Attribution attribution
) {
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
