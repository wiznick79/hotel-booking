package pt.hotelbooking.booking.payment;

import pt.hotelbooking.booking.model.entity.PaymentMethod;

public interface PaymentProvider {
    PaymentProviderType providerType();

    boolean supports(PaymentMethod paymentMethod);

    PaymentInitiation initiate(PaymentInitiationRequest request);

    PaymentWebhookResult verifyWebhook(PaymentWebhook webhook);
}
