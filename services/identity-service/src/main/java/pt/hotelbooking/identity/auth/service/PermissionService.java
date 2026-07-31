package pt.hotelbooking.identity.auth.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import pt.hotelbooking.identity.auth.model.Permission;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    public List<String> permissionsFor(Iterable<? extends GrantedAuthority> authorities) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);

        for (GrantedAuthority authority : authorities) {
            addPermissionsForRole(permissions, authority.getAuthority());

            addPermissionClaim(permissions, authority.getAuthority());
        }

        return permissions.stream()
                .map(Enum::name)
                .toList();
    }

    private void addPermissionClaim(Set<Permission> permissions, String authority) {
        try {
            permissions.add(Permission.valueOf(authority));
        } catch (IllegalArgumentException exception) {
            // The authority is a role or an unknown value.
        }
    }

    private void addPermissionsForRole(Set<Permission> permissions, String role) {
        switch (role) {
            case "ROLE_ADMIN" -> permissions.addAll(EnumSet.allOf(Permission.class));
            case "ROLE_STAFF" -> permissions.addAll(EnumSet.of(
                    Permission.HOTEL_READ,
                    Permission.ROOM_READ,
                    Permission.ROOM_MANAGE,
                    Permission.RESERVATION_READ,
                    Permission.RESERVATION_MANAGE,
                    Permission.NOTIFICATION_MANAGE));
            case "ROLE_MANAGER" -> permissions.addAll(EnumSet.of(
                    Permission.HOTEL_READ,
                    Permission.HOTEL_MANAGE,
                    Permission.ROOM_READ,
                    Permission.ROOM_MANAGE,
                    Permission.ROOM_TYPE_MANAGE,
                    Permission.RATE_PERIOD_MANAGE,
                    Permission.RESERVATION_READ,
                    Permission.RESERVATION_CREATE,
                    Permission.RESERVATION_MANAGE,
                    Permission.BOOKING_POLICY_MANAGE,
                    Permission.DISCOUNT_CODE_MANAGE,
                    Permission.STAFF_MANAGE,
                    Permission.NOTIFICATION_MANAGE));
            case "ROLE_CUSTOMER" -> permissions.addAll(EnumSet.of(
                    Permission.HOTEL_READ,
                    Permission.ROOM_READ,
                    Permission.RESERVATION_CREATE));
            default -> {
                // Unknown roles receive no permissions.
            }
        }
    }
}
