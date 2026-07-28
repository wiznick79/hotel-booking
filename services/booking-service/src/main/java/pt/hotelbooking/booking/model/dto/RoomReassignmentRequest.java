package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RoomReassignmentRequest(@NotBlank String currentRoomId,
                                      @NotBlank String replacementRoomId) {
}
