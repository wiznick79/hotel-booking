package pt.hotelbooking.booking.model.dto;

import pt.hotelbooking.booking.model.entity.ReservationItem;

import java.util.UUID;

public record ReservationItemResponse(UUID id, String roomTypeId, String roomId) {
    public static ReservationItemResponse from(ReservationItem item) {
        return new ReservationItemResponse(item.getId(), item.getRoomTypeId(), item.getRoomId());
    }
}
