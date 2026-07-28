package pt.hotelbooking.booking.model.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTests {

    @Test
    void guestAccessIsValidUntilItIsRevoked() {
        Reservation reservation = new Reservation(
                "hotel-1",
                "Guest",
                "+351000000000",
                "guest@example.com",
                2,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                null);
        reservation.configureGuestAccess("hash", Instant.now().plusSeconds(3600));

        assertThat(reservation.hasValidGuestAccess(Instant.now())).isTrue();

        reservation.revokeGuestAccess();

        assertThat(reservation.hasValidGuestAccess(Instant.now())).isFalse();
    }

    @Test
    void cancellationInvalidatesGuestAccess() {
        Reservation reservation = new Reservation(
                "hotel-1",
                "Guest",
                "+351000000000",
                null,
                1,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                null);
        reservation.configureGuestAccess("hash", Instant.now().plusSeconds(3600));

        reservation.cancel();

        assertThat(reservation.hasValidGuestAccess(Instant.now())).isFalse();
    }

    @Test
    void storesDiscountSnapshot() {
        Reservation reservation = new Reservation(
                "hotel-1",
                "Guest",
                "+351000000000",
                null,
                1,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                null);

        reservation.applyPriceSnapshot(BigDecimal.valueOf(180), "EUR");
        reservation.applyDiscountSnapshot("SUMMER", BigDecimal.valueOf(20));

        assertThat(reservation.getTotalPrice()).isEqualByComparingTo("180");
        assertThat(reservation.getDiscountCode()).isEqualTo("SUMMER");
        assertThat(reservation.getDiscountAmount()).isEqualByComparingTo("20");
    }
}
