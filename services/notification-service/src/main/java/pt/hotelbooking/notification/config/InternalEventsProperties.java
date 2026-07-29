package pt.hotelbooking.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-events")
public record InternalEventsProperties(String serviceToken) {
}
