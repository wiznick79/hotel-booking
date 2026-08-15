package pt.hotelbooking.notification.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactMessageRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptValidContactMessage() {
        assertThat(validator.validate(validRequest("guest@example.com"))).isEmpty();
    }

    @Test
    void shouldRejectMalformedEmailAddresses() {
        assertThat(validator.validate(validRequest("guest@example"))).isNotEmpty();
        assertThat(validator.validate(validRequest("guest..name@example.com"))).isNotEmpty();
        assertThat(validator.validate(validRequest("guest@example..com"))).isNotEmpty();
        assertThat(validator.validate(validRequest("guest @example.com"))).isNotEmpty();
        assertThat(validator.validate(validRequest(
                "guest@example.com\r\nBcc: attacker@example.com")))
                .isNotEmpty();
    }

    @Test
    void shouldRejectUnexpectedCharactersInOtherFields() {
        ContactMessageRequest invalidRequest = new ContactMessageRequest(
                UUID.randomUUID(),
                "Guest\r\nBcc: attacker@example.com",
                "guest@example.com",
                "call me<script>",
                ContactSubject.GENERAL,
                "A valid-looking message with a null character \u0000 inside.",
                true,
                "");

        assertThat(validator.validate(invalidRequest)).hasSize(3);
    }

    private ContactMessageRequest validRequest(String email) {
        return new ContactMessageRequest(
                UUID.randomUUID(),
                "Guest Name",
                email,
                "+351 123 456 789",
                ContactSubject.BOOKING,
                "I have a question about my upcoming stay.",
                true,
                "");
    }
}
