package pt.hotelbooking.notification.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HotelServiceResilienceTests {

    @Test
    void retriesTransientHotelServiceFailure() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .recordException(HotelServiceUnavailableException.class::isInstance)
                .build();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .retryExceptions(HotelServiceUnavailableException.class)
                .build();
        HotelServiceResilience resilience = new HotelServiceResilience(
                CircuitBreakerRegistry.of(circuitBreakerConfig),
                RetryRegistry.of(retryConfig));
        AtomicInteger attempts = new AtomicInteger();

        String result = resilience.execute("retrieve contact details", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new HotelServiceUnavailableException("temporary failure");
            }

            return "available";
        });

        assertThat(result).isEqualTo("available");
        assertThat(attempts).hasValue(2);
    }
}
