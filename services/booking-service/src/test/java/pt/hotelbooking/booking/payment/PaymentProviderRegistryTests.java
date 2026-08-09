package pt.hotelbooking.booking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

class PaymentProviderRegistryTests {

    @Test
    void returnsProviderThatSupportsRequestedPaymentMethod() {
        PaymentProvider stripeProvider = new FakePaymentProvider(
                PaymentProviderType.STRIPE,
                List.of(PaymentMethod.CARD, PaymentMethod.MULTIBANCO, PaymentMethod.MB_WAY));
        PaymentProvider paypalProvider = new FakePaymentProvider(
                PaymentProviderType.PAYPAL,
                List.of(PaymentMethod.PAYPAL));
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(stripeProvider, paypalProvider));

        assertThat(registry.providerFor(PaymentMethod.MB_WAY)).isSameAs(stripeProvider);
        assertThat(registry.providerFor(PaymentMethod.PAYPAL)).isSameAs(paypalProvider);
    }

    @Test
    void rejectsMethodWithoutConfiguredProvider() {
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of());

        assertThatThrownBy(() -> registry.providerFor(PaymentMethod.CARD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No payment provider is configured for CARD.");
    }

    private record FakePaymentProvider(PaymentProviderType providerType, List<PaymentMethod> methods)
            implements PaymentProvider {

        @Override
        public boolean supports(PaymentMethod paymentMethod) {
            return methods.contains(paymentMethod);
        }

        @Override
        public PaymentInitiation initiate(PaymentInitiationRequest request) {
            return new PaymentInitiation(providerType, "provider-payment", "https://example.test/checkout", java.util.Map.of());
        }

        @Override
        public PaymentWebhookResult verifyWebhook(PaymentWebhook webhook) {
            return new PaymentWebhookResult("provider-payment", PaymentWebhookResult.PaymentOutcome.SUCCEEDED);
        }
    }
}
