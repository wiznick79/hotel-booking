package pt.hotelbooking.notification.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreatedEvent(
        UUID reservationId,
        String guestEmail,
        String guestName,
        String encryptedGuestAccessToken,
        String hotelId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal totalPrice,
        String currency) {
}
