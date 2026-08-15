package pt.hotelbooking.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.hotelbooking.notification.integration.HotelContactClient;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.model.dto.ContactMessageRequest;
import pt.hotelbooking.notification.model.dto.ContactSubject;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTests {

    @Mock
    private HotelContactClient hotelContactClient;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSender emailSender;

    private ContactMessageService contactMessageService;

    private final UUID hotelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contactMessageService = new ContactMessageService(
                hotelContactClient,
                notificationRepository,
                emailSender);
        ReflectionTestUtils.setField(contactMessageService, "maximumAttempts", 3);
        ReflectionTestUtils.setField(contactMessageService, "defaultRecipient", "");
    }

    @Test
    void shouldSendContactMessageToConfiguredHotelAddress() {
        when(hotelContactClient.getHotel(hotelId)).thenReturn(hotel());

        var response = contactMessageService.send(request());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(emailSender).send(notificationCaptor.getValue());

        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isEqualTo("reservations@hotel.test");
        assertThat(notification.getSenderReplyToAddress()).isEqualTo("guest@example.com");
        assertThat(notification.getSubject()).isEqualTo("Booking question · Test Hotel");
        assertThat(notification.getBody()).contains("Guest Name", "+351 123 456 789", "late arrival");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.status()).isEqualTo("SENT");
    }

    @Test
    void shouldKeepMessagePendingWhenEmailDeliveryTemporarilyFails() {
        when(hotelContactClient.getHotel(hotelId)).thenReturn(hotel());
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailSender)
                .send(any(Notification.class));

        var response = contactMessageService.send(request());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notificationCaptor.getValue().getAttempts()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("PENDING");
    }

    private ContactMessageRequest request() {
        return new ContactMessageRequest(
                hotelId,
                "Guest Name",
                "guest@example.com",
                "+351 123 456 789",
                ContactSubject.BOOKING,
                "I have a question about a late arrival.",
                true,
                "");
    }

    private HotelContactClient.HotelContactDetails hotel() {
        return new HotelContactClient.HotelContactDetails(
                hotelId,
                "Test Hotel",
                "Test Hotel",
                "no-reply@hotel.test",
                "reservations@hotel.test",
                true);
    }
}
