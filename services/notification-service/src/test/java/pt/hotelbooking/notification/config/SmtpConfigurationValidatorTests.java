package pt.hotelbooking.notification.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SmtpConfigurationValidatorTests {

    @Test
    void shouldAllowMailpitConfigurationWithoutAuthentication() {
        assertThatNoException().isThrownBy(() -> new SmtpConfigurationValidator(
                "mailpit",
                "",
                "",
                false,
                false,
                false));
    }

    @Test
    void shouldRejectAuthenticationWithoutCredentials() {
        assertThatIllegalStateException().isThrownBy(() -> new SmtpConfigurationValidator(
                "email-smtp.eu-west-3.amazonaws.com",
                "smtp-user",
                "",
                true,
                true,
                true))
                .withMessage("SMTP username and password are required when SMTP authentication is enabled.");
    }

    @Test
    void shouldRejectRequiredStartTlsWhenDisabled() {
        assertThatIllegalStateException().isThrownBy(() -> new SmtpConfigurationValidator(
                "email-smtp.eu-west-3.amazonaws.com",
                "smtp-user",
                "smtp-password",
                true,
                false,
                true))
                .withMessage("STARTTLS must be enabled when STARTTLS is required.");
    }
}
