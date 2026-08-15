package pt.hotelbooking.booking.payment.local;

import java.util.EnumSet;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.payment.PaymentInitiation;
import pt.hotelbooking.booking.payment.PaymentInitiationRequest;
import pt.hotelbooking.booking.payment.PaymentProvider;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.payment.PaymentWebhook;
import pt.hotelbooking.booking.payment.PaymentWebhookResult;

@Component
@ConditionalOnProperty(name = "payment.local-simulation.enabled", havingValue = "true")
public class LocalSimulationPaymentProvider implements PaymentProvider {
    private static final EnumSet<PaymentMethod> SUPPORTED_METHODS = EnumSet.of(
            PaymentMethod.CARD,
            PaymentMethod.PAYPAL,
            PaymentMethod.MULTIBANCO,
            PaymentMethod.MB_WAY);

    private final String publicFrontendBaseUrl;

    public LocalSimulationPaymentProvider(
            @Value("${payment.local-simulation.public-frontend-base-url}") String publicFrontendBaseUrl) {
        this.publicFrontendBaseUrl = publicFrontendBaseUrl.replaceAll("/$", "");
    }

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.LOCAL_SIMULATION;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod);
    }

    @Override
    public PaymentInitiation initiate(PaymentInitiationRequest request) {
        String providerPaymentId = UUID.randomUUID().toString();
        String redirectUrl = publicFrontendBaseUrl + "/#/payment/simulated/" + providerPaymentId;

        return new PaymentInitiation(providerType(), providerPaymentId, null, redirectUrl, java.util.Map.of());
    }

    @Override
    public PaymentWebhookResult verifyWebhook(PaymentWebhook webhook) {
        throw new UnsupportedOperationException("The local simulation does not use signed webhooks.");
    }
}
