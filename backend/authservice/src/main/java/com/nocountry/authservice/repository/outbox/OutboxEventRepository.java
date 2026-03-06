package com.nocountry.authservice.repository.outbox;

import com.nocountry.authservice.domain.outbox.OutboxEvent;
import com.nocountry.authservice.domain.outbox.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status IN :statuses
              AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findReadyToPublish(
            @Param("statuses") List<OutboxEventStatus> statuses,
            @Param("now") OffsetDateTime now
    );
}
