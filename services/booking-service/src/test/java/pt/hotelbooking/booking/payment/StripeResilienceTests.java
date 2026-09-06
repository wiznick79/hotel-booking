package pt.hotelbooking.booking.payment;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import pt.hotelbooking.booking.payment.stripe.StripeResilience;
import pt.hotelbooking.booking.payment.stripe.StripeServiceUnavailableException;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeResilienceTests {
    @Test
    void opensCircuitAfterRepeatedStripeFailures() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordException(StripeServiceUnavailableException.class::isInstance).build();
        StripeResilience resilience = new StripeResilience(
                CircuitBreakerRegistry.of(config), BulkheadRegistry.ofDefaults());

        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> resilience.execute(() -> {
                throw new StripeServiceUnavailableException("provider failure");
            })).isInstanceOf(StripeServiceUnavailableException.class);
        }
        assertThatThrownBy(() -> resilience.execute(() -> "not called"))
                .isInstanceOf(StripeServiceUnavailableException.class)
                .hasMessageContaining("circuit is open");
    }

    @Test
    void rejectsExcessConcurrentStripeCallsWithoutWaiting() throws Exception {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build();
        StripeResilience resilience = new StripeResilience(
                CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.of(config));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> resilience.execute(() -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            }));
            entered.await();
            assertThatThrownBy(() -> resilience.execute(() -> "rejected"))
                    .isInstanceOf(StripeServiceUnavailableException.class)
                    .hasMessageContaining("busy");
            release.countDown();
            first.get();
        }
    }
}
