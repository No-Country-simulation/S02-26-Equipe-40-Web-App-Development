package com.nocountry.conversionflow.conversionflow_api.infrastructure.messaging.idempotency;

public interface EventIdempotencyStore {

    boolean tryMarkProcessed(String idempotencyKey);
}
