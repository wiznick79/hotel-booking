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

    private String hotelId;

    private String recipient;

    private String subject;

    private String senderDisplayName;

    private String senderFromAddress;

    private String senderReplyToAddress;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private int attempts;

    private Instant lastAttemptAt;

    @Column(columnDefinition = "text")
    private String lastError;

    public Notification(UUID reservationId, String hotelId, String recipient, String subject, String body,
                        String senderDisplayName, String senderFromAddress, String senderReplyToAddress) {
        this.reservationId = reservationId;
        this.hotelId = hotelId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.senderDisplayName = senderDisplayName;
        this.senderFromAddress = senderFromAddress;
        this.senderReplyToAddress = senderReplyToAddress;
    }

    public Notification(UUID reservationId, String hotelId, String recipient, String subject, String body) {
        this(reservationId, hotelId, recipient, subject, body, null, null, null);
    }

    public void markSent() {
        status = NotificationStatus.SENT;
        attempts++;
        lastAttemptAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error, int maximumAttempts) {
        attempts++;
        lastAttemptAt = Instant.now();
        lastError = error;
        status = attempts >= maximumAttempts ? NotificationStatus.FAILED : NotificationStatus.PENDING;
    }

    public void requeue() {
        status = NotificationStatus.PENDING;
        attempts = 0;
        lastAttemptAt = null;
        lastError = null;
    }
}
