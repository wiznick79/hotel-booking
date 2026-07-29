package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(

        @NotBlank
        String username,

        @NotBlank
        @Size(min = 12)
        String password,

        Set<String> roles,

        @jakarta.validation.constraints.NotEmpty
        Set<UUID> hotelIds) {
}
