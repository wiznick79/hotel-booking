package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.hotelbooking.hotel.model.entity.RoomStatus;

import java.util.UUID;

public record RoomUpdateRequest(@NotNull UUID roomTypeId, @NotBlank String roomNumber,
                                @NotNull @Min(0) Integer floor, @NotNull RoomStatus status) {
}
