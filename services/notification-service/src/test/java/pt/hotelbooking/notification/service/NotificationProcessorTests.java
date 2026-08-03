package pt.hotelbooking.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.hotelbooking.notification.event.ReservationCreatedEvent;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private GuestAccessTokenCipher guestAccessTokenCipher;

    @InjectMocks
    private NotificationProcessor notificationProcessor;

    @Test
    void shouldCreateNotificationForReservationWithEmail() {
        ReservationCreatedEvent event = event("guest@example.com");

        when(notificationRepository.existsByReservationIdAndSubject(
                event.reservationId(), "Hotel booking confirmation")).thenReturn(false);

        notificationProcessor.processReservationCreated(event);

        verify(notificationRepository).save(argThat(notification ->
                notification.getSenderDisplayName().equals("Hotel Morgadinha")
                        && notification.getSenderFromAddress().equals("morgadinha@wiznick.net")
                        && notification.getSenderReplyToAddress().equals("reservas@hotelmorgadinha.pt")));
    }

    @Test
    void shouldIgnoreReservationWithoutEmail() {
        notificationProcessor.processReservationCreated(event(null));

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldIgnoreDuplicateReservationEvent() {
        ReservationCreatedEvent event = event("guest@example.com");

        when(notificationRepository.existsByReservationIdAndSubject(
                event.reservationId(), "Hotel booking confirmation")).thenReturn(true);

        notificationProcessor.processReservationCreated(event);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldUsePublicFrontendHashRouteForGuestAccessLink() {
        ReservationCreatedEvent event = event("guest@example.com", "encrypted-token");
        ReflectionTestUtils.setField(notificationProcessor, "publicFrontendBaseUrl", "https://hotel.example");
        when(notificationRepository.existsByReservationIdAndSubject(
                event.reservationId(), "Hotel booking confirmation")).thenReturn(false);
        when(guestAccessTokenCipher.decrypt("encrypted-token")).thenReturn("guest-token");

        notificationProcessor.processReservationCreated(event);

        verify(notificationRepository).save(argThat(notification -> notification.getBody()
                .contains("https://hotel.example/#/booking/guest-token")));
    }

    @Test
    void shouldSendPendingNotificationThroughEmailSender() {
        Notification notification = new Notification(
                UUID.randomUUID(), "hotel-1", "guest@example.com", "Subject", "Message");
        ReflectionTestUtils.setField(notificationProcessor, "maximumAttempts", 3);
        when(notificationRepository.findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, 3))
                .thenReturn(List.of(notification));

        notificationProcessor.deliverPendingNotifications();

        verify(emailSender).send(notification);
        org.assertj.core.api.Assertions.assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    private ReservationCreatedEvent event(String email) {
        return event(email, null);
    }

    private ReservationCreatedEvent event(String email, String encryptedGuestAccessToken) {
        return new ReservationCreatedEvent(
                UUID.randomUUID(),
                email,
                "Guest",
                encryptedGuestAccessToken,
                "hotel-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                BigDecimal.valueOf(100),
                "EUR",
                "Hotel Morgadinha",
                "Hotel Morgadinha",
                "morgadinha@wiznick.net",
                "reservas@hotelmorgadinha.pt");
    }
}
