package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(

        @NotBlank
        @Size(max = 120)
        String fullName) {
}
