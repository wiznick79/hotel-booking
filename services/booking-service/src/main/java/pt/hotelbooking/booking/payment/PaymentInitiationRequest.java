package pt.hotelbooking.booking.payment;

import java.math.BigDecimal;
import java.util.UUID;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

public record PaymentInitiationRequest(
        UUID reservationId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String returnUrl,
        String cancelUrl) { }
