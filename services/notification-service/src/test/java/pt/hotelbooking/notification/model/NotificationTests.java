package pt.hotelbooking.notification.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTests {

    @Test
    void shouldRequeueFailedNotificationWithCleanRetryState() {
        Notification notification = new Notification(
                UUID.randomUUID(), "hotel-1", "guest@example.com", "Subject", "Body");
        notification.markFailed("SMTP unavailable", 1);

        notification.requeue();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getAttempts()).isZero();
        assertThat(notification.getLastAttemptAt()).isNull();
        assertThat(notification.getLastError()).isNull();
    }
}
