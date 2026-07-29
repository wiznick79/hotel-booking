package pt.hotelbooking.booking.model.dto;

import pt.hotelbooking.booking.model.entity.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        String eventType,
        UUID aggregateId,
        Instant createdAt,
        int attempts,
        Instant nextAttemptAt,
        Instant failedAt,
        String lastError) {

    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getCreatedAt(),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getFailedAt(),
                event.getLastError());
    }
}
