package pt.hotelbooking.booking.payment;

import java.util.UUID;

public record PaymentWebhookResult(
        String providerPaymentId,
        String providerPaymentIntentId,
        UUID paymentAttemptId,
        PaymentOutcome outcome,
        PaymentInstructions instructions) {

    public PaymentWebhookResult(String providerPaymentId, PaymentOutcome outcome) {
        this(providerPaymentId, null, null, outcome, null);
    }

    public enum PaymentOutcome {
        SUCCEEDED,
        FAILED,
        PENDING,
        CANCELLED
    }
}
