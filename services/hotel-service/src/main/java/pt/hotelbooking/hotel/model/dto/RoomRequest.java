package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoomRequest(@NotNull UUID hotelId, @NotNull UUID roomTypeId,
                          @NotBlank String roomNumber, @NotNull @Min(0) Integer floor) {
}
