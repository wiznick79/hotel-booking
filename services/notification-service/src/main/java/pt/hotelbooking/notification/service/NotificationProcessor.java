package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import pt.hotelbooking.notification.event.ReservationCreatedEvent;
import pt.hotelbooking.notification.event.ReservationNotificationEvent;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;

    private final GuestAccessTokenCipher guestAccessTokenCipher;

    private final EmailSender emailSender;

    @Value("${public-frontend.base-url:http://localhost:3000}")
    private String publicFrontendBaseUrl;

    @Value("${notification.maximum-attempts:3}")
    private int maximumAttempts;

    @Transactional
    public void processReservationCreated(ReservationCreatedEvent event) {
        process(event.reservationId(), event.hotelId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                "ReservationCreated", event.encryptedGuestAccessToken(), event.hotelName(),
                event.notificationDisplayName(), event.notificationFromAddress(),
                event.notificationReplyToAddress());
    }

    @Transactional
    public void processReservationEvent(ReservationNotificationEvent event) {
        process(event.reservationId(), event.hotelId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                event.eventType(), null, event.hotelName(), event.notificationDisplayName(),
                event.notificationFromAddress(), event.notificationReplyToAddress());
    }

    private void process(java.util.UUID reservationId, String hotelId, String guestEmail, String guestName,
                         java.time.LocalDate checkInDate, java.time.LocalDate checkOutDate,
                         java.math.BigDecimal totalPrice, String currency, String eventType,
                         String encryptedGuestAccessToken, String hotelName, String senderDisplayName,
                         String senderFromAddress, String senderReplyToAddress) {
        if (guestEmail == null || guestEmail.isBlank()) {
            return;
        }

        String subject = subjectFor(eventType);
        if (notificationRepository.existsByReservationIdAndSubject(reservationId, subject)) {
            return;
        }

        String accessLink = encryptedGuestAccessToken == null ? ""
                : " Guest access link: " + publicFrontendBaseUrl + "/#/booking/"
                + guestAccessTokenCipher.decrypt(encryptedGuestAccessToken);
        String hotelNameForMessage = hotelName == null || hotelName.isBlank() ? "the hotel" : hotelName;

        notificationRepository.save(new Notification(
                reservationId,
                hotelId,
                guestEmail,
                subject,
                "Hello " + guestName + ", your reservation at " + hotelNameForMessage + " is: " + eventType
                        + ". Stay: " + checkInDate + " to " + checkOutDate
                        + ". Total: " + totalPrice + " " + currency + "." + accessLink,
                senderDisplayName, senderFromAddress, senderReplyToAddress));
    }

    private String subjectFor(String eventType) {
        return switch (eventType) {
            case "ReservationModified" -> "Hotel booking modified";
            case "ReservationConfirmed" -> "Hotel booking confirmed";
            case "ReservationCancelled" -> "Hotel booking cancelled";
            case "ReservationHoldExpired" -> "Hotel booking hold expired";
            default -> "Hotel booking confirmation";
        };
    }

    @Transactional
    @Scheduled(fixedDelayString = "${notification.retry-delay-ms:60000}")
    public void deliverPendingNotifications() {
        notificationRepository.findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, maximumAttempts)
                .forEach(this::deliver);
    }

    private void deliver(Notification notification) {
        try {
            log.info("Delivering notification {} with subject {}", notification.getId(), notification.getSubject());
            emailSender.send(notification);
            notification.markSent();
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage(), maximumAttempts);
        }
    }
}
