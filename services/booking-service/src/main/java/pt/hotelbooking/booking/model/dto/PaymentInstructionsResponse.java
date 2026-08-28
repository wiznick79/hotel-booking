package pt.hotelbooking.booking.model.dto;

import java.time.Instant;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;

public record PaymentInstructionsResponse(
        String entity,
        String reference,
        String hostedVoucherUrl,
        Instant expiresAt) {

    public static PaymentInstructionsResponse from(PaymentAttempt attempt) {
        if (attempt.getPaymentEntity() == null && attempt.getPaymentReference() == null
                && attempt.getHostedVoucherUrl() == null) {
            return null;
        }

        return new PaymentInstructionsResponse(
                attempt.getPaymentEntity(),
                attempt.getPaymentReference(),
                attempt.getHostedVoucherUrl(),
                attempt.getPaymentInstructionsExpireAt());
    }
}
