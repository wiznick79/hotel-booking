package pt.hotelbooking.booking;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import pt.hotelbooking.booking.repository.ReservationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "jwt.secret=test-secret-that-is-long-enough-for-hmac-sha256",
        "booking.outbox.dispatch-delay-ms=3600000"
})
class PostgreSqlFlywayIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("booking_test")
            .withUsername("hotel_booking")
            .withPassword("hotel_booking");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReservationRepository reservationRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void shouldApplyBookingSchemaToPostgreSql() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(tableExists("reservations")).isTrue();
        assertThat(tableExists("reservation_items")).isTrue();
        assertThat(tableExists("booking_policies")).isTrue();
        assertThat(tableExists("discount_codes")).isTrue();
        assertThat(tableExists("outbox_events")).isTrue();
    }

    @Test
    void shouldExecuteReservationOverlapQueryOnPostgreSql() {
        Reservation reservation = new Reservation(
                "hotel-1",
                "Guest",
                "+351900000000",
                "guest@example.com",
                2,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 15),
                null);

        reservation.addRoom("room-1");
        reservationRepository.saveAndFlush(reservation);

        boolean blocking = reservationRepository.hasBlockingReservation(
                "room-1",
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 12),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                Instant.now());

        assertThat(blocking).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                Integer.class,
                tableName);

        return count != null && count == 1;
    }
}
