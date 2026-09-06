package pt.hotelbooking.booking.payment.stripe;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class StripeResilience {
    static final String BACKEND_NAME = "stripe";

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public StripeResilience(CircuitBreakerRegistry circuitBreakerRegistry, BulkheadRegistry bulkheadRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND_NAME);
        this.bulkhead = bulkheadRegistry.bulkhead(BACKEND_NAME);
    }

    public <T> T execute(Supplier<T> request) {
        Supplier<T> circuitProtected = CircuitBreaker.decorateSupplier(circuitBreaker, request);
        Supplier<T> isolated = Bulkhead.decorateSupplier(bulkhead, circuitProtected);

        try {
            return isolated.get();
        } catch (CallNotPermittedException exception) {
            throw new StripeServiceUnavailableException("Stripe is temporarily unavailable because its circuit is open.", exception);
        } catch (BulkheadFullException exception) {
            throw new StripeServiceUnavailableException("Stripe is temporarily busy. Please try again shortly.", exception);
        }
    }
}
