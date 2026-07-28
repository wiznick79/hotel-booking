package pt.hotelbooking.booking.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(UUID id) { super("Reservation not found: " + id); }

    public ReservationNotFoundException(String message) { super(message); }
}
