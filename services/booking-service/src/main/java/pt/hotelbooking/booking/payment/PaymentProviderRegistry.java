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
        List<PaymentProvider> matchingProviders = providers.stream()
                .filter(provider -> provider.supports(paymentMethod))
                .toList();

        if (matchingProviders.isEmpty()) {
            throw new IllegalStateException("No payment provider is configured for " + paymentMethod + ".");
        }

        if (matchingProviders.size() > 1) {
            throw new IllegalStateException(
                    "More than one payment provider is configured for " + paymentMethod + ".");
        }

        return matchingProviders.getFirst();
    }

    public PaymentProvider providerFor(PaymentProviderType providerType) {
        return providers.stream()
                .filter(provider -> provider.providerType() == providerType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No payment provider is configured for " + providerType + "."));
    }

    public boolean hasExactlyOneProviderFor(PaymentMethod paymentMethod) {
        return providers.stream()
                .filter(provider -> provider.supports(paymentMethod))
                .count() == 1;
    }
}
