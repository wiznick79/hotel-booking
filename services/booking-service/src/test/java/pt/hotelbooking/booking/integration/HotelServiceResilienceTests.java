package pt.hotelbooking.booking.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HotelServiceResilienceTests {

    @Test
    void retriesOneTransientFailure() {
        HotelServiceResilience resilience = resilience(5, 50, 2);
        AtomicInteger attempts = new AtomicInteger();

        String result = resilience.execute("retrieve hotel", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new HotelServiceUnavailableException("temporary failure");
            }

            return "available";
        });

        assertThat(result).isEqualTo("available");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void opensCircuitAndRejectsCallsWithoutInvokingDependency() {
        HotelServiceResilience resilience = resilience(2, 100, 1);
        AtomicInteger attempts = new AtomicInteger();

        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> resilience.execute("retrieve hotel", () -> {
                attempts.incrementAndGet();
                throw new HotelServiceUnavailableException("temporary failure");
            })).isInstanceOf(HotelServiceUnavailableException.class);
        }

        assertThatThrownBy(() -> resilience.execute("retrieve hotel", () -> {
            attempts.incrementAndGet();
            return "unexpected";
        }))
                .isInstanceOf(HotelServiceUnavailableException.class)
                .hasMessageContaining("circuit is open");

        assertThat(attempts).hasValue(2);
    }

    private HotelServiceResilience resilience(
            int minimumCalls,
            float failureThreshold,
            int retryAttempts) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(minimumCalls)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordException(HotelServiceUnavailableException.class::isInstance)
                .build();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryAttempts)
                .waitDuration(Duration.ZERO)
                .retryExceptions(HotelServiceUnavailableException.class)
                .build();

        return new HotelServiceResilience(
                CircuitBreakerRegistry.of(circuitBreakerConfig),
                RetryRegistry.of(retryConfig));
    }
}
