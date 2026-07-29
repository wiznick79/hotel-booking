package pt.hotelbooking.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.notification.event.ReservationCreatedEvent;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTests {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationProcessor notificationProcessor;

    @Test
    void shouldCreateNotificationForReservationWithEmail() {
        ReservationCreatedEvent event = event("guest@example.com");

        when(notificationRepository.existsByReservationIdAndSubject(
                event.reservationId(), "Hotel booking confirmation")).thenReturn(false);

        notificationProcessor.processReservationCreated(event);

        verify(notificationRepository).save(any(Notification.class));
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

    private ReservationCreatedEvent event(String email) {
        return new ReservationCreatedEvent(
                UUID.randomUUID(),
                email,
                "Guest",
                null,
                "hotel-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                BigDecimal.valueOf(100),
                "EUR");
    }
}
