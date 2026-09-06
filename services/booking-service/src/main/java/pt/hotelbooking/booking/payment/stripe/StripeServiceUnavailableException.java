package pt.hotelbooking.booking.payment.stripe;

public class StripeServiceUnavailableException extends RuntimeException {
    public StripeServiceUnavailableException(String message) {
        super(message);
    }

    public StripeServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
