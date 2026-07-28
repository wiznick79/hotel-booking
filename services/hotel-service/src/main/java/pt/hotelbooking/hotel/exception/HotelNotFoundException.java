package pt.hotelbooking.hotel.exception;

import java.util.UUID;

public class HotelNotFoundException extends RuntimeException {

    public HotelNotFoundException(UUID id) {
        super("Hotel not found: " + id);
    }
}
