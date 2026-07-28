package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.notification.event.ReservationCreatedEvent;
import pt.hotelbooking.notification.event.ReservationNotificationEvent;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void processReservationCreated(ReservationCreatedEvent event) {
        process(event.reservationId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                "ReservationCreated");
    }

    @Transactional
    public void processReservationEvent(ReservationNotificationEvent event) {
        process(event.reservationId(), event.guestEmail(), event.guestName(),
                event.checkInDate(), event.checkOutDate(), event.totalPrice(), event.currency(),
                event.eventType());
    }

    private void process(java.util.UUID reservationId, String guestEmail, String guestName,
                         java.time.LocalDate checkInDate, java.time.LocalDate checkOutDate,
                         java.math.BigDecimal totalPrice, String currency, String eventType) {
        if (guestEmail == null || guestEmail.isBlank()) {
            return;
        }

        String subject = subjectFor(eventType);
        if (notificationRepository.existsByReservationIdAndSubject(reservationId, subject)) {
            return;
        }

        notificationRepository.save(new Notification(
                reservationId,
                guestEmail,
                subject,
                "Hello " + guestName + ", your reservation event is: " + eventType
                        + ". Stay: " + checkInDate + " to " + checkOutDate
                        + ". Total: " + totalPrice + " " + currency));
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
            System.out.println("Sending notification to " + notification.getRecipient()
                    + ": " + notification.getSubject());
            notification.markSent();
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage());
        }
    }
}
