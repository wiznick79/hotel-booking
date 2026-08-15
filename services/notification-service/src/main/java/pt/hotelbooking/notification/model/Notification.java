package pt.hotelbooking.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
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

    private UUID referenceId;

    private String hotelId;

    private String recipient;

    private String subject;

    private String senderDisplayName;

    private String senderFromAddress;

    private String senderReplyToAddress;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    @Column(columnDefinition = "text")
    private String contactMessage;

    private Instant createdAt;

    private Instant readAt;

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
        this.referenceId = reservationId;
        this.hotelId = hotelId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.senderDisplayName = senderDisplayName;
        this.senderFromAddress = senderFromAddress;
        this.senderReplyToAddress = senderReplyToAddress;
        this.notificationType = NotificationType.RESERVATION;
    }

    public Notification(UUID reservationId, String hotelId, String recipient, String subject, String body) {
        this(reservationId, hotelId, recipient, subject, body, null, null, null);
    }

    public Notification(UUID referenceId, String recipient, String subject, String body) {
        this.referenceId = referenceId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.notificationType = NotificationType.ACCOUNT_VERIFICATION;
    }

    public static Notification contactMessage(
            UUID referenceId,
            String hotelId,
            String recipient,
            String subject,
            String body,
            String senderDisplayName,
            String senderFromAddress,
            String senderReplyToAddress,
            String contactName,
            String contactEmail,
            String contactPhone,
            String contactMessage) {
        Notification notification = new Notification(referenceId, recipient, subject, body);
        notification.hotelId = hotelId;
        notification.senderDisplayName = senderDisplayName;
        notification.senderFromAddress = senderFromAddress;
        notification.senderReplyToAddress = senderReplyToAddress;
        notification.notificationType = NotificationType.CONTACT_MESSAGE;
        notification.contactName = contactName;
        notification.contactEmail = contactEmail;
        notification.contactPhone = contactPhone;
        notification.contactMessage = contactMessage;
        return notification;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
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
