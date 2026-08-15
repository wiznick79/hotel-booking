package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.notification.config.HotelScopeAuthorization;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationType;
import pt.hotelbooking.notification.model.dto.AdminContactMessageResponse;
import pt.hotelbooking.notification.model.dto.ContactMessagePageResponse;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactMessageManagementService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public ContactMessagePageResponse findByHotel(
            String hotelId,
            int page,
            int size,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return ContactMessagePageResponse.from(
                notificationRepository.findByHotelIdAndNotificationTypeOrderByCreatedAtDesc(
                        hotelId,
                        NotificationType.CONTACT_MESSAGE,
                        PageRequest.of(safePage, safeSize)));
    }

    @Transactional
    public AdminContactMessageResponse markRead(
            UUID messageId,
            Authentication authentication) {
        Notification notification = findContactMessage(messageId);
        HotelScopeAuthorization.requireAccess(authentication, notification.getHotelId());
        notification.markRead();

        return AdminContactMessageResponse.from(notification);
    }

    private Notification findContactMessage(UUID messageId) {
        Notification notification = notificationRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Contact message not found."));

        if (notification.getNotificationType() != NotificationType.CONTACT_MESSAGE) {
            throw new IllegalArgumentException("Contact message not found.");
        }

        return notification;
    }
}
