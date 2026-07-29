package pt.hotelbooking.identity.auth.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record UpdateHotelAssignmentsRequest(@NotEmpty Set<UUID> hotelIds) {
}
