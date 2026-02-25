package com.nocountry.conversionflow.conversionflow_api.infrastructure;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import com.nocountry.conversionflow.conversionflow_api.service.google.GoogleConversionsService;
import com.nocountry.conversionflow.conversionflow_api.service.meta.MetaConversionsService;
import com.nocountry.conversionflow.conversionflow_api.service.pipedrive.PipedriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ConversionDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConversionDispatchScheduler.class);
    private static final int MAX_ATTEMPTS = 5;

    private final ConversionDispatchRepository repository;
    private final GoogleConversionsService googleService;
    private final MetaConversionsService metaService;
    private final PipedriveService pipedriveService;

    public ConversionDispatchScheduler(
            ConversionDispatchRepository repository,
            GoogleConversionsService googleService,
            MetaConversionsService metaService,
            PipedriveService pipedriveService
    ) {
        this.repository = repository;
        this.googleService = googleService;
        this.metaService = metaService;
        this.pipedriveService = pipedriveService;
    }

    @Scheduled(fixedDelay = 60000)
    public void processPendingDispatches() {
        String cycleId = UUID.randomUUID().toString();
        long startedAt = System.currentTimeMillis();

        List<ConversionDispatch> dispatches =
                repository.findByStatusIn(List.of(DispatchStatus.PENDING, DispatchStatus.FAILED));

        log.info("dispatch.cycle.started cycleId={} pendingOrFailedCount={}", cycleId, dispatches.size());

        int successCount = 0;
        int failedCount = 0;
        int skippedMaxAttemptsCount = 0;

        for (ConversionDispatch dispatch : dispatches) {

            if (!dispatch.canRetry(MAX_ATTEMPTS)) {
                skippedMaxAttemptsCount++;
                log.warn("dispatch.skipped.maxAttempts cycleId={} dispatchId={} provider={} attempts={} maxAttempts={}",
                        cycleId, dispatch.getId(), dispatch.getProvider(), dispatch.getAttemptCount(), MAX_ATTEMPTS);
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

                switch (dispatch.getProvider()) {
                    case GOOGLE -> googleService.sendConversionFromPayload(dispatch.getPayload());
                    case META -> metaService.sendConversionFromPayload(dispatch.getPayload());
                    case PIPEDRIVE -> pipedriveService.syncFromPayload(dispatch.getPayload());
                }

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
