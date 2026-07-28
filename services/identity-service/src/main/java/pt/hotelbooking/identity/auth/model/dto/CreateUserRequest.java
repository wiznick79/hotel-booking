package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank
        String username,

        @NotBlank
        @Size(min = 12)
        String password,

        Set<String> roles) {
}
