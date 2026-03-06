package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.config.properties.DispatchRetryProperties;
import com.nocountry.conversionflow.conversionflow_api.application.port.ConversionDispatchHandler;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProcessDispatchQueueUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDispatchQueueUseCase.class);

    private final ConversionDispatchRepository repository;
    private final DispatchRetryProperties dispatchRetryProperties;
    private final Map<Provider, ConversionDispatchHandler> providerHandlers;

    public ProcessDispatchQueueUseCase(
            ConversionDispatchRepository repository,
            DispatchRetryProperties dispatchRetryProperties,
            List<ConversionDispatchHandler> providerHandlers
    ) {
        this.repository = repository;
        this.dispatchRetryProperties = dispatchRetryProperties;
        this.providerHandlers = new EnumMap<>(Provider.class);
        for (ConversionDispatchHandler providerHandler : providerHandlers) {
            this.providerHandlers.put(providerHandler.provider(), providerHandler);
        }
    }

    @Transactional
    public void execute() {
        String cycleId = UUID.randomUUID().toString();
        long startedAt = System.currentTimeMillis();

        List<ConversionDispatch> dispatches =
                repository.findByStatusIn(List.of(DispatchStatus.PENDING, DispatchStatus.FAILED));

        log.info("dispatch.cycle.started cycleId={} pendingOrFailedCount={}", cycleId, dispatches.size());

        int successCount = 0;
        int failedCount = 0;
        int skippedMaxAttemptsCount = 0;

        for (ConversionDispatch dispatch : dispatches) {

            if (!dispatch.canRetry(dispatchRetryProperties.getMaxAttempts())) {
                skippedMaxAttemptsCount++;
                log.warn("dispatch.skipped.maxAttempts cycleId={} dispatchId={} provider={} attempts={} maxAttempts={}",
                        cycleId,
                        dispatch.getId(),
                        dispatch.getProvider(),
                        dispatch.getAttemptCount(),
                        dispatchRetryProperties.getMaxAttempts());
                continue;
            }

            try {
                log.info("dispatch.processing cycleId={} dispatchId={} leadId={} provider={} status={} attempts={}",
                        cycleId,
                        dispatch.getId(),
                        dispatch.getLeadId(),
                        dispatch.getProvider(),
                        dispatch.getStatus(),
                        dispatch.getAttemptCount());

                ConversionDispatchHandler providerHandler = providerHandlers.get(dispatch.getProvider());
                if (providerHandler == null) {
                    throw new IllegalStateException("No handler configured for provider " + dispatch.getProvider());
                }
                providerHandler.dispatch(dispatch.getPayload());

                dispatch.markSuccess();
                successCount++;
                log.info("dispatch.success cycleId={} dispatchId={} provider={} attempts={}",
                        cycleId, dispatch.getId(), dispatch.getProvider(), dispatch.getAttemptCount());

            } catch (Exception e) {
                dispatch.markFailure(e.getMessage());
                failedCount++;
                log.error("dispatch.failure cycleId={} dispatchId={} provider={} attempts={} error={}",
                        cycleId,
                        dispatch.getId(),
                        dispatch.getProvider(),
                        dispatch.getAttemptCount(),
                        e.getMessage(),
                        e);
            }

            repository.save(dispatch);
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info("dispatch.cycle.finished cycleId={} total={} success={} failed={} skippedMaxAttempts={} elapsedMs={}",
                cycleId,
                dispatches.size(),
                successCount,
                failedCount,
                skippedMaxAttemptsCount,
                elapsedMs);
    }
}
