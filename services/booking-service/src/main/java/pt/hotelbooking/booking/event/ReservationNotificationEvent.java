package pt.hotelbooking.booking.event;

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
        String currency,
        String hotelName,
        String notificationDisplayName,
        String notificationFromAddress,
        String notificationReplyToAddress) {

    public ReservationNotificationEvent(String eventType, UUID reservationId, String hotelId,
                                        String guestEmail, String guestName, LocalDate checkInDate,
                                        LocalDate checkOutDate, BigDecimal totalPrice, String currency) {
        this(eventType, reservationId, hotelId, guestEmail, guestName, checkInDate, checkOutDate,
                totalPrice, currency, null, null, null, null);
    }
}
