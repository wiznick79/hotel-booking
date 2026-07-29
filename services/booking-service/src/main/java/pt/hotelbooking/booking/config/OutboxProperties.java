package pt.hotelbooking.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking.outbox")
public record OutboxProperties(
        int maximumAttempts,
        long initialRetryDelaySeconds,
        long maximumRetryDelaySeconds) {
}
