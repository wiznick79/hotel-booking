package pt.hotelbooking.notification.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationNotificationEvent(
        String eventType,
        UUID reservationId,
        String hotelId,
        String guestEmail,
        String guestName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal totalPrice,
        String currency) {
}
