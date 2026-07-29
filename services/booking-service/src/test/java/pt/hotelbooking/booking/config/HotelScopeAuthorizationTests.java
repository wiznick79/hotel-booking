package pt.hotelbooking.booking.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HotelScopeAuthorizationTests {

    @Test
    void shouldAllowAssignedHotel() {
        assertThatCode(() -> HotelScopeAuthorization.requireAccess(
                authentication(List.of("hotel-a", "hotel-b")), "hotel-b"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUnassignedHotel() {
        assertThatThrownBy(() -> HotelScopeAuthorization.requireAccess(
                authentication(List.of("hotel-a")), "hotel-b"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRejectTokenWithoutHotelAssignments() {
        assertThatThrownBy(() -> HotelScopeAuthorization.requireAccess(authentication(null), "hotel-a"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private JwtAuthenticationToken authentication(List<String> hotelIds) {
        Map<String, Object> claims = hotelIds == null
                ? Map.of("sub", "staff-user")
                : Map.of("sub", "staff-user", "hotelIds", hotelIds);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), claims);

        return new JwtAuthenticationToken(jwt);
    }
}
