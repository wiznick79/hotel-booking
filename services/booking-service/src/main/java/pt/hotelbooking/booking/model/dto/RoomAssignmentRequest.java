package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RoomAssignmentRequest(@NotBlank String roomId) {
}
