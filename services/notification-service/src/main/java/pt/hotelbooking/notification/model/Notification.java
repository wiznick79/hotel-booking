package pt.hotelbooking.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID reservationId;

    private String recipient;

    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private int attempts;

    private Instant lastAttemptAt;

    @Column(columnDefinition = "text")
    private String lastError;

    public Notification(UUID reservationId, String recipient, String subject, String body) {
        this.reservationId = reservationId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
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
