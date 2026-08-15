package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(

        @NotBlank
        @Size(max = 120)
        String fullName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 10)
        String password) {
}
