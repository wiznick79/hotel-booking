package pt.hotelbooking.identity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "identity_outbox_events")
@Getter
@NoArgsConstructor
public class IdentityOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType;

    private Long aggregateId;

    @Column(columnDefinition = "text")
    private String payload;

    private Instant createdAt = Instant.now();

    private Instant publishedAt;

    private int attempts;

    private Instant nextAttemptAt = Instant.now();

    private Instant failedAt;

    @Column(columnDefinition = "text")
    private String lastError;

    public IdentityOutboxEvent(String eventType, Long aggregateId, String payload) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void markPublished() {
        publishedAt = Instant.now();
        attempts++;
        lastError = null;
    }

    public void markAttempted(String error, Instant nextAttemptAt, int maximumAttempts) {
        attempts++;
        lastError = error;

        if (attempts >= maximumAttempts) {
            failedAt = Instant.now();
            return;
        }

        this.nextAttemptAt = nextAttemptAt;
    }
}
