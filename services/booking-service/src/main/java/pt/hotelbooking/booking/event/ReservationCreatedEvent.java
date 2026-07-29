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
        String currency) {

    public ReservationCreatedEvent(UUID reservationId, String hotelId, LocalDate checkInDate,
                                   LocalDate checkOutDate, BigDecimal totalPrice, String currency) {
        this(reservationId, null, null, null, hotelId, checkInDate, checkOutDate, totalPrice, currency);
    }
}
