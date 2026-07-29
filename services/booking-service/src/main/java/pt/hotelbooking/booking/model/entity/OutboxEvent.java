package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType;

    private UUID aggregateId;

    @Column(columnDefinition = "text")
    private String payload;

    private Instant createdAt = Instant.now();

    private Instant publishedAt;

    private int attempts;

    private Instant nextAttemptAt = Instant.now();

    private Instant failedAt;

    @Column(columnDefinition = "text")
    private String lastError;

    public OutboxEvent(String eventType, UUID aggregateId, String payload) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void markPublished() {
        publishedAt = Instant.now();
        attempts++;
        lastError = null;
    }

    public void markAttempted(String error, Instant nextAttempt, int maximumAttempts) {
        attempts++;
        lastError = error;

        if (attempts >= maximumAttempts) {
            failedAt = Instant.now();
            return;
        }

        nextAttemptAt = nextAttempt;
    }

    public void requeue() {
        attempts = 0;
        nextAttemptAt = Instant.now();
        failedAt = null;
        lastError = null;
    }
}
