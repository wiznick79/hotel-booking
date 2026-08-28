package pt.hotelbooking.notification.integration;

public class HotelServiceUnavailableException extends RuntimeException {

    public HotelServiceUnavailableException(String message) {
        super(message);
    }

    public HotelServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
