package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 10)
        String newPassword) {
}
