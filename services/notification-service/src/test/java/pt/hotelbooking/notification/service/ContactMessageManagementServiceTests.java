package pt.hotelbooking.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationType;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactMessageManagementServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void shouldReturnOnlyContactMessagesForAssignedHotel() {
        String hotelId = UUID.randomUUID().toString();
        Notification message = contactMessage(hotelId);
        when(notificationRepository.findByHotelIdAndNotificationTypeOrderByCreatedAtDesc(
                eq(hotelId),
                eq(NotificationType.CONTACT_MESSAGE),
                isA(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));
        ContactMessageManagementService service = new ContactMessageManagementService(
                notificationRepository);

        var result = service.findByHotel(hotelId, 0, 20, authentication(hotelId));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().email()).isEqualTo("guest@example.com");
    }

    @Test
    void shouldMarkContactMessageAsRead() {
        String hotelId = UUID.randomUUID().toString();
        Notification message = contactMessage(hotelId);
        when(notificationRepository.findById(message.getReferenceId()))
                .thenReturn(Optional.of(message));
        ContactMessageManagementService service = new ContactMessageManagementService(
                notificationRepository);

        var result = service.markRead(message.getReferenceId(), authentication(hotelId));

        assertThat(result.readAt()).isNotNull();
        verify(notificationRepository).findById(message.getReferenceId());
    }

    @Test
    void shouldRejectAccessToMessageFromAnotherHotel() {
        String hotelId = UUID.randomUUID().toString();
        Notification message = contactMessage(hotelId);
        when(notificationRepository.findById(message.getReferenceId()))
                .thenReturn(Optional.of(message));
        ContactMessageManagementService service = new ContactMessageManagementService(
                notificationRepository);

        assertThatThrownBy(() -> service.markRead(
                message.getReferenceId(),
                authentication(UUID.randomUUID().toString())))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Notification contactMessage(String hotelId) {
        UUID referenceId = UUID.randomUUID();

        return Notification.contactMessage(
                referenceId,
                hotelId,
                "hotel@example.com",
                "General enquiry · Test Hotel",
                "Email body",
                "Test Hotel",
                "no-reply@example.com",
                "guest@example.com",
                "Guest Name",
                "guest@example.com",
                null,
                "A question about the hotel.");
    }

    private JwtAuthenticationToken authentication(String hotelId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("staff")
                .claim("hotelIds", List.of(hotelId))
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}
