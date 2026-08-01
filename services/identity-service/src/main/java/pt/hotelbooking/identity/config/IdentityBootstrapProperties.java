package pt.hotelbooking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.bootstrap")
public record IdentityBootstrapProperties(String username, String password) {
}
