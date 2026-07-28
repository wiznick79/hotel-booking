package pt.hotelbooking.hotel.exception;

import java.util.UUID;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(UUID id) {
        super("Room type not found: " + id);
    }
}
