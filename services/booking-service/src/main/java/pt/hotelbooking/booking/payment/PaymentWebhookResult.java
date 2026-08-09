package pt.hotelbooking.booking.payment;

public record PaymentWebhookResult(
        String providerPaymentId,
        PaymentOutcome outcome) {

    public enum PaymentOutcome {
        SUCCEEDED,
        FAILED,
        PENDING,
        CANCELLED
    }
}
