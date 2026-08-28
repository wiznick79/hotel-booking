package pt.hotelbooking.booking.integration;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class HotelServiceResilience {

    private static final String BACKEND_NAME = "hotelService";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public HotelServiceResilience(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND_NAME);
        this.retry = retryRegistry.retry(BACKEND_NAME);
    }

    public <T> T execute(String operation, Supplier<T> request) {
        Supplier<T> guardedRequest = CircuitBreaker.decorateSupplier(circuitBreaker, request);
        guardedRequest = Retry.decorateSupplier(retry, guardedRequest);

        try {
            return guardedRequest.get();
        } catch (CallNotPermittedException exception) {
            throw new HotelServiceUnavailableException(
                    "Hotel service circuit is open while attempting to " + operation + ".",
                    exception);
        }
    }
}
