package pt.hotelbooking.booking.payment;

import java.math.BigDecimal;
import java.util.UUID;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

public record PaymentInitiationRequest(
        UUID reservationId,
        UUID paymentAttemptId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String guestEmail,
        String returnUrl,
        String cancelUrl) {

    public PaymentInitiationRequest(
            UUID reservationId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String guestEmail,
            String returnUrl,
            String cancelUrl) {
        this(reservationId, null, amount, currency, paymentMethod, guestEmail, returnUrl, cancelUrl);
    }
}
