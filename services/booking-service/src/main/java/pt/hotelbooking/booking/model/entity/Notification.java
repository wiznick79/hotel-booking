package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID reservationId;

    private String recipient;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private int attempts;

    private Instant lastAttemptAt;

    private String lastError;

    public Notification(UUID reservationId, String recipient) {
        this.reservationId = reservationId;
        this.recipient = recipient;
    }

    public void markSent() {
        status = NotificationStatus.SENT;
        attempts++;
        lastAttemptAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        status = NotificationStatus.FAILED;
        attempts++;
        lastAttemptAt = Instant.now();
        lastError = error;
    }
}
