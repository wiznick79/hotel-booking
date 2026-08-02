package pt.hotelbooking.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmtpConfigurationValidator {

    public SmtpConfigurationValidator(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean smtpAuthenticationEnabled,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired) {
        if (host.isBlank()) {
            throw new IllegalStateException("SMTP host must be configured.");
        }

        if (smtpAuthenticationEnabled && (username.isBlank() || password.isBlank())) {
            throw new IllegalStateException(
                    "SMTP username and password are required when SMTP authentication is enabled.");
        }

        if (startTlsRequired && !startTlsEnabled) {
            throw new IllegalStateException(
                    "STARTTLS must be enabled when STARTTLS is required.");
        }
    }
}
