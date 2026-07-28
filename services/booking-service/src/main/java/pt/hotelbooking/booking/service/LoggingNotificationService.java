package pt.hotelbooking.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import pt.hotelbooking.booking.model.entity.Notification;
import pt.hotelbooking.booking.model.entity.NotificationStatus;
import pt.hotelbooking.booking.repository.NotificationRepository;

import java.util.List;
import pt.hotelbooking.booking.model.entity.Reservation;

@Service
@Profile({"dev", "test"})
public class LoggingNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final EmailSender emailSender;

    public LoggingNotificationService(
            NotificationRepository notificationRepository,
            EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
    }

    @Override
    @Async
    public void sendGuestAccessLink(Reservation reservation, String rawToken) {
        Notification notification = notificationRepository.save(
                new Notification(reservation.getId(), reservation.getGuestEmail()));

        deliver(notification, rawToken);
    }

    @Scheduled(fixedDelayString = "${booking.notifications.retry-delay-ms:60000}")
    public void retryFailedNotifications() {
        List<Notification> notifications = notificationRepository.findByStatusAndAttemptsLessThan(
                NotificationStatus.FAILED,
                3);

        notifications.forEach(notification -> deliver(notification, null));
    }

    private void deliver(Notification notification, String rawToken) {
        try {
            if (rawToken == null) {
                throw new IllegalStateException("Notification payload is not available for retry.");
            }

            emailSender.sendGuestAccessLink(
                    notification.getRecipient(),
                    notification.getReservationId().toString(),
                    rawToken);
            notification.markSent();
        } catch (RuntimeException exception) {
            notification.markFailed(exception.getMessage());
        }

        notificationRepository.save(notification);
    }
}
