package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.RoomUnavailability;

import java.time.LocalDate;
import java.util.UUID;

public record RoomUnavailabilityResponse(
        UUID id,
        UUID roomId,
        LocalDate fromDate,
        LocalDate toDate,
        String reason,
        boolean emergency) {

    public static RoomUnavailabilityResponse from(RoomUnavailability unavailability) {
        return new RoomUnavailabilityResponse(
                unavailability.getId(),
                unavailability.getRoom().getId(),
                unavailability.getFromDate(),
                unavailability.getToDate(),
                unavailability.getReason(),
                unavailability.isEmergency());
    }
}
