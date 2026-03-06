package com.nocountry.conversionflow.conversionflow_api.infrastructure.messaging.idempotency;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEventIdempotencyStore implements EventIdempotencyStore {

    private final Map<String, Instant> processedKeys = new ConcurrentHashMap<>();

    @Override
    public boolean tryMarkProcessed(String idempotencyKey) {
        pruneOldEntries();
        return processedKeys.putIfAbsent(idempotencyKey, Instant.now()) == null;
    }

    private void pruneOldEntries() {
        Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60);
        processedKeys.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
