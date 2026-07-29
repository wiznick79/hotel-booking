package pt.hotelbooking.hotel.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

public class HotelScopeAuthorization {

    private HotelScopeAuthorization() {
    }

    public static void requireAccess(Authentication authentication, java.util.UUID hotelId) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !hotelIds(jwtAuthentication).contains(hotelId.toString())) {
            throw new AccessDeniedException("You are not assigned to this hotel.");
        }
    }

    private static List<String> hotelIds(JwtAuthenticationToken authentication) {
        List<String> hotelIds = authentication.getToken().getClaimAsStringList("hotelIds");
        return hotelIds == null ? List.of() : hotelIds;
    }
}
