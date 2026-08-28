package pt.hotelbooking.notification.model.dto;

import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminContactMessageResponse(
        UUID id,
        UUID referenceId,
        String hotelId,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        NotificationStatus deliveryStatus,
        Instant receivedAt,
        Instant readAt) {

    public static AdminContactMessageResponse from(Notification notification) {
        return new AdminContactMessageResponse(
                notification.getId(),
                notification.getReferenceId(),
                notification.getHotelId(),
                notification.getContactName(),
                notification.getContactEmail(),
                notification.getContactPhone(),
                notification.getSubject(),
                notification.getContactMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
