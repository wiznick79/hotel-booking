package pt.hotelbooking.booking.payment;

import java.time.Instant;

public record PaymentInstructions(
        String entity,
        String reference,
        String hostedVoucherUrl,
        Instant expiresAt) { }
