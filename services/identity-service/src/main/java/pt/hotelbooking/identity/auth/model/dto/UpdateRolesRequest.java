package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(

        @NotEmpty
        Set<String> roles) {
}
