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

    @Value("${public-frontend.base-url:http://localhost:3000}")
    private String publicFrontendBaseUrl;

    @Transactional
    public void processReservationCreated(ReservationCreatedEvent event) {
        process(event.reservationId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                "ReservationCreated", event.encryptedGuestAccessToken());
    }

    @Transactional
    public void processReservationEvent(ReservationNotificationEvent event) {
        process(event.reservationId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                event.eventType(), null);
    }

    private void process(java.util.UUID reservationId, String guestEmail, String guestName,
                         java.time.LocalDate checkInDate, java.time.LocalDate checkOutDate,
                         java.math.BigDecimal totalPrice, String currency, String eventType,
                         String encryptedGuestAccessToken) {
        if (guestEmail == null || guestEmail.isBlank()) {
            return;
        }

        String subject = subjectFor(eventType);
        if (notificationRepository.existsByReservationIdAndSubject(reservationId, subject)) {
            return;
        }

        String accessLink = encryptedGuestAccessToken == null ? ""
                : " Guest access link: " + publicFrontendBaseUrl + "/reservations/guest/"
                + guestAccessTokenCipher.decrypt(encryptedGuestAccessToken);

        notificationRepository.save(new Notification(
                reservationId,
                guestEmail,
                subject,
                "Hello " + guestName + ", your reservation event is: " + eventType
                        + ". Stay: " + checkInDate + " to " + checkOutDate
                        + ". Total: " + totalPrice + " " + currency + "." + accessLink));
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
        notificationRepository.findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, 3)
                .forEach(this::deliver);
    }

    private void deliver(Notification notification) {
        try {
            log.info("Delivering notification {} with subject {}", notification.getId(), notification.getSubject());
            notification.markSent();
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage());
        }
    }
}
