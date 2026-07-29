package pt.hotelbooking.booking;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "jwt.secret=booking-integration-secret-that-is-long-enough-for-hmac-sha256",
                "booking.outbox.dispatch-delay-ms=3600000"
        })
class BookingAuthorizationHttpIntegrationTests {

    private static final String JWT_SECRET =
            "booking-integration-secret-that-is-long-enough-for-hmac-sha256";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("booking_authorization_test")
            .withUsername("hotel_booking")
            .withPassword("hotel_booking");

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void shouldRejectProtectedEndpointWithoutJwt() {
        ResponseEntity<String> response = restTemplate.getForEntity(affectedReservationsUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectJwtWithoutRequiredPermission() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                affectedReservationsUrl(),
                org.springframework.http.HttpMethod.GET,
                authorizedRequest(List.of()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAuthorizeJwtWithRequiredPermission() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                affectedReservationsUrl(),
                org.springframework.http.HttpMethod.GET,
                authorizedRequest(List.of("RESERVATION_READ")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    private String affectedReservationsUrl() {
        return "http://localhost:" + port
                + "/api/reservations/room/room-1/affected?from=2026-08-10&to=2026-08-12&hotelId=hotel-1";
    }

    private HttpEntity<Void> authorizedRequest(List<String> permissions) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(createJwt(permissions));

        return new HttpEntity<>(headers);
    }

    private String createJwt(List<String> permissions) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("staff-user")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("permissions", permissions)
                .claim("hotelIds", List.of("hotel-1"))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));

        return jwt.serialize();
    }
}
