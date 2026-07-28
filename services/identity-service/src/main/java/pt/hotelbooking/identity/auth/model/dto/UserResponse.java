package pt.hotelbooking.identity.auth.model.dto;

import pt.hotelbooking.identity.auth.model.entity.IdentityUser;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        boolean enabled,
        Set<String> roles) {

    public static UserResponse from(IdentityUser user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                roleNames);
    }
}
