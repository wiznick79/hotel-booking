package pt.hotelbooking.identity.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pt.hotelbooking.identity.auth.service.PermissionService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionServiceTests {

    private final PermissionService permissionService = new PermissionService();

    @Test
    void adminReceivesAllPermissions() {
        List<String> permissions = permissionService.permissionsFor(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(permissions)
                .contains("HOTEL_MANAGE", "ROOM_MANAGE", "RESERVATION_MANAGE");
    }

    @Test
    void staffReceivesOperationalPermissions() {
        List<String> permissions = permissionService.permissionsFor(List.of(
                new SimpleGrantedAuthority("ROLE_STAFF")));

        assertThat(permissions)
                .contains("RESERVATION_READ", "RESERVATION_MANAGE")
                .doesNotContain("HOTEL_MANAGE");
    }
}
