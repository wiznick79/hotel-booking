package pt.hotelbooking.notification.event;

import java.util.UUID;

public record CustomerRegistrationRequestedEvent(
        UUID eventId,
        String email,
        String encryptedVerificationToken) {
}
