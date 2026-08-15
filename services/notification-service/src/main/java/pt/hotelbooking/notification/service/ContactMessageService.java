package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.notification.integration.HotelContactClient;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.dto.ContactMessageRequest;
import pt.hotelbooking.notification.model.dto.ContactMessageResponse;
import pt.hotelbooking.notification.model.dto.ContactSubject;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final HotelContactClient hotelContactClient;
    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

    @Value("${notification.maximum-attempts:3}")
    private int maximumAttempts;

    @Value("${notification.contact.default-recipient:}")
    private String defaultRecipient;

    @Transactional
    public ContactMessageResponse send(ContactMessageRequest request) {
        HotelContactClient.HotelContactDetails hotel = hotelContactClient.getHotel(request.hotelId());
        String guestName = request.name().trim();
        String guestEmail = request.email().trim().toLowerCase();
        String guestPhone = normalizeNullable(request.phone());
        String guestMessage = normalizeMessage(request.message());
        String recipient = firstNonBlank(
                hotel.notificationReplyToAddress(),
                hotel.notificationFromAddress(),
                defaultRecipient);

        if (recipient == null) {
            throw new IllegalStateException("The hotel has no contact email address configured.");
        }

        UUID referenceId = UUID.randomUUID();
        Notification notification = Notification.contactMessage(
                referenceId,
                hotel.id().toString(),
                recipient,
                subjectFor(request.subject(), hotel.name()),
                bodyFor(guestName, guestEmail, guestPhone, guestMessage),
                hotel.notificationDisplayName(),
                hotel.notificationFromAddress(),
                guestEmail,
                guestName,
                guestEmail,
                guestPhone,
                guestMessage);

        notificationRepository.save(notification);

        try {
            emailSender.send(notification);
            notification.markSent();
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage(), maximumAttempts);
        }

        return new ContactMessageResponse(referenceId, notification.getStatus().name());
    }

    private String subjectFor(ContactSubject subject, String hotelName) {
        String category = switch (subject) {
            case BOOKING -> "Booking question";
            case ACCESSIBILITY -> "Accessibility question";
            default -> "General enquiry";
        };

        return category + " · " + hotelName;
    }

    private String bodyFor(
            String guestName,
            String guestEmail,
            String guestPhone,
            String guestMessage) {
        return "New website enquiry\n\n"
                + "Name: " + guestName + "\n"
                + "Email: " + guestEmail + "\n"
                + "Phone: " + (guestPhone == null ? "Not provided" : guestPhone) + "\n\n"
                + guestMessage;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeMessage(String message) {
        return message.trim()
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}
