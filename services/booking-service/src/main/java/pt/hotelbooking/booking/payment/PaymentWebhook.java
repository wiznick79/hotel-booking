package pt.hotelbooking.booking.payment;

import java.util.Map;

public record PaymentWebhook(
        String signature,
        String payload,
        Map<String, String> headers) { }
