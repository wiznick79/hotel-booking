package pt.hotelbooking.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryIntegrationTests {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void shouldFindBlockingReservationWhenDatesOverlap() {
        Reservation reservation = createReservation("room-1", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 15));

        reservationRepository.saveAndFlush(reservation);

        boolean blocking = reservationRepository.hasBlockingReservation(
                "room-1",
                LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 12),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                Instant.now());

        assertThat(blocking).isTrue();
    }

    @Test
    void shouldNotFindBlockingReservationWhenNewStayStartsOnCheckoutDate() {
        Reservation reservation = createReservation("room-1", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 15));

        reservationRepository.saveAndFlush(reservation);

        boolean blocking = reservationRepository.hasBlockingReservation(
                "room-1",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 15),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                Instant.now());

        assertThat(blocking).isFalse();
    }

    private Reservation createReservation(String roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        Reservation reservation = new Reservation(
                "hotel-1",
                "Guest",
                "+351900000000",
                "guest@example.com",
                2,
                checkInDate,
                checkOutDate,
                null);

        reservation.addRoom(roomId);
        return reservation;
    }
}
