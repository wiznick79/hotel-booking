package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.Room;
import pt.hotelbooking.hotel.model.entity.RoomStatus;

import java.util.UUID;

public record RoomResponse(UUID id, UUID hotelId, UUID roomTypeId, String roomNumber,
                           Integer floor, RoomStatus status, boolean active) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getHotel().getId(), room.getRoomType().getId(),
                room.getRoomNumber(), room.getFloor(), room.getStatus(), room.isActive());
    }
}
