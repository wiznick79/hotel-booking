package pt.hotelbooking.booking.payment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

@Service
@RequiredArgsConstructor
public class PaymentProviderRegistry {
    private final List<PaymentProvider> providers;

    public PaymentProvider providerFor(PaymentMethod paymentMethod) {
        return providers.stream()
                .filter(provider -> provider.supports(paymentMethod))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No payment provider is configured for " + paymentMethod + "."));
    }
}
