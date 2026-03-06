package com.nocountry.authservice.service.outbox;

import com.nocountry.authservice.domain.outbox.OutboxEvent;
import com.nocountry.authservice.domain.outbox.OutboxEventStatus;
import com.nocountry.authservice.repository.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxEventRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final int maxAttempts;
    private final long baseBackoffMs;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter publishRetryCounter;

    public OutboxEventRelayService(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            MeterRegistry meterRegistry,
            @Value("${app.outbox.exchange:auth.events}") String exchange,
            @Value("${app.outbox.routing-key:user.registered.v1}") String routingKey,
            @Value("${app.outbox.max-attempts:5}") int maxAttempts,
            @Value("${app.outbox.base-backoff-ms:1000}") long baseBackoffMs
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMs = baseBackoffMs;
        this.publishSuccessCounter = meterRegistry.counter("outbox.publish.success");
        this.publishFailureCounter = meterRegistry.counter("outbox.publish.failure");
        this.publishRetryCounter = meterRegistry.counter("outbox.publish.retry");
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> candidates = outboxEventRepository.findReadyToPublish(
                List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
                OffsetDateTime.now()
        );

        for (OutboxEvent event : candidates) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayloadJson(), message -> {
                MessageProperties properties = message.getMessageProperties();
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                properties.setHeader("eventId", event.getId().toString());
                properties.setHeader("eventType", event.getEventType());
                properties.setHeader("idempotencyKey", event.getIdempotencyKey());
                return message;
            });

            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(OffsetDateTime.now());
            event.setErrorReason(null);
            outboxEventRepository.save(event);
            publishSuccessCounter.increment();
            log.info("outbox.publish.success eventId={} eventType={}", event.getId(), event.getEventType());
        } catch (Exception exception) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setErrorReason(exception.getMessage());

            if (attempts >= maxAttempts) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setNextAttemptAt(null);
                publishFailureCounter.increment();
                log.error("outbox.publish.failed.final eventId={} attempts={} reason={}", event.getId(), attempts, exception.getMessage());
            } else {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setNextAttemptAt(OffsetDateTime.now().plusNanos(baseBackoffMs * 1_000_000L * attempts));
                publishRetryCounter.increment();
                log.warn("outbox.publish.retry eventId={} attempts={} reason={}", event.getId(), attempts, exception.getMessage());
            }

            outboxEventRepository.save(event);
        }
    }
}
