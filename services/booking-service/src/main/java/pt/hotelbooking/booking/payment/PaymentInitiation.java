package pt.hotelbooking.booking.payment;

import java.util.Map;

public record PaymentInitiation(
        PaymentProviderType provider,
        String providerPaymentId,
        String providerPaymentIntentId,
        String redirectUrl,
        Map<String, String> customerInstructions) { }
