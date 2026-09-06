package pt.hotelbooking.notification.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

@Component
public class SmtpResilience {
    static final String BACKEND_NAME = "smtp";

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public SmtpResilience(CircuitBreakerRegistry circuitBreakerRegistry, BulkheadRegistry bulkheadRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND_NAME);
        this.bulkhead = bulkheadRegistry.bulkhead(BACKEND_NAME);
    }

    public void execute(Runnable request) {
        Runnable circuitProtected = CircuitBreaker.decorateRunnable(circuitBreaker, request);
        Runnable isolated = Bulkhead.decorateRunnable(bulkhead, circuitProtected);

        try {
            isolated.run();
        } catch (CallNotPermittedException exception) {
            throw new EmailDeliveryUnavailableException("Email delivery is temporarily unavailable because its circuit is open.", exception);
        } catch (BulkheadFullException exception) {
            throw new EmailDeliveryUnavailableException("Email delivery is temporarily busy.", exception);
        }
    }
}
