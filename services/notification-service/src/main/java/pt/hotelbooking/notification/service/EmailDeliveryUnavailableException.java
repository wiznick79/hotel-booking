package pt.hotelbooking.notification.service;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
