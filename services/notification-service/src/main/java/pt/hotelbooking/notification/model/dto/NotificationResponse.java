package pt.hotelbooking.notification.model.dto;

import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID reservationId,
        String hotelId,
        String recipient,
        String subject,
        NotificationStatus status,
        int attempts,
        Instant lastAttemptAt,
        String lastError) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getReservationId(),
                notification.getHotelId(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getStatus(),
                notification.getAttempts(),
                notification.getLastAttemptAt(),
                notification.getLastError());
    }
}
