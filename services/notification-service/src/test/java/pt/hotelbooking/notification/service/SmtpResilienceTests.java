package pt.hotelbooking.notification.service;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpResilienceTests {
    @Test
    void opensCircuitAndStopsCallingSmtp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordException(EmailDeliveryUnavailableException.class::isInstance).build();
        SmtpResilience resilience = new SmtpResilience(
                CircuitBreakerRegistry.of(config), BulkheadRegistry.ofDefaults());
        AtomicInteger attempts = new AtomicInteger();

        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> resilience.execute(() -> {
                attempts.incrementAndGet();
                throw new EmailDeliveryUnavailableException("SMTP failed", null);
            })).isInstanceOf(EmailDeliveryUnavailableException.class);
        }
        assertThatThrownBy(() -> resilience.execute(attempts::incrementAndGet))
                .isInstanceOf(EmailDeliveryUnavailableException.class)
                .hasMessageContaining("circuit is open");
        assertThat(attempts).hasValue(2);
    }
}
