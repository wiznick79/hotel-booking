package pt.hotelbooking.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "internal-events.service-token=notification-integration-token",
                "notification.retry-delay-ms=3600000"
        })
class NotificationEventHttpIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("notification_http_test")
            .withUsername("hotel_booking")
            .withPassword("hotel_booking");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldRejectInternalEventWithoutServiceToken() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                url("/internal/events/reservation-created"),
                new HttpEntity<>(reservationCreatedEvent()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void shouldPersistNotificationForAuthorizedInternalEvent() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Token", "notification-integration-token");

        ResponseEntity<Void> response = restTemplate.postForEntity(
                url("/internal/events/reservation-created"),
                new HttpEntity<>(reservationCreatedEvent(), headers),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String reservationCreatedEvent() {
        return """
                {
                  "reservationId": "%s",
                  "guestEmail": "guest@example.com",
                  "guestName": "Guest",
                  "checkInDate": "2026-08-10",
                  "checkOutDate": "2026-08-12",
                  "totalPrice": 120.00,
                  "currency": "EUR"
                }
                """.formatted(UUID.randomUUID());
    }
}
