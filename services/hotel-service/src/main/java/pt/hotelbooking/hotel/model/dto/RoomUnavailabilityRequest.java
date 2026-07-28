package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RoomUnavailabilityRequest(
        @NotNull UUID roomId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @Size(max = 500) String reason,
        boolean emergency) {
}
