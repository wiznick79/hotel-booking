package pt.hotelbooking.booking.event;

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
        String currency,
        String hotelName,
        String notificationDisplayName,
        String notificationFromAddress,
        String notificationReplyToAddress) {

    public ReservationCreatedEvent(UUID reservationId, String hotelId, LocalDate checkInDate,
                                   LocalDate checkOutDate, BigDecimal totalPrice, String currency) {
        this(reservationId, null, null, null, hotelId, checkInDate, checkOutDate, totalPrice, currency,
                null, null, null, null);
    }

    public ReservationCreatedEvent(UUID reservationId, String guestEmail, String guestName,
                                   String encryptedGuestAccessToken, String hotelId,
                                   LocalDate checkInDate, LocalDate checkOutDate,
                                   BigDecimal totalPrice, String currency) {
        this(reservationId, guestEmail, guestName, encryptedGuestAccessToken, hotelId, checkInDate,
                checkOutDate, totalPrice, currency, null, null, null, null);
    }
}
