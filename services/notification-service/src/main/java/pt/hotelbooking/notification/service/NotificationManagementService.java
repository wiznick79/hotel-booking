package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.hotelbooking.notification.config.HotelScopeAuthorization;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.model.dto.NotificationResponse;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationManagementService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByHotel(String hotelId, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);

        return notificationRepository.findAllByOrderByLastAttemptAtDesc().stream()
                .filter(notification -> hotelId.equals(notification.getHotelId()))
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse requeue(UUID notificationId, Authentication authentication) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."));

        HotelScopeAuthorization.requireAccess(authentication, notification.getHotelId());

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only terminally failed notifications can be requeued.");
        }

        notification.requeue();
        return NotificationResponse.from(notification);
    }
}
