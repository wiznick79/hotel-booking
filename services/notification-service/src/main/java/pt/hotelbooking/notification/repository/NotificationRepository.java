package pt.hotelbooking.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;
import pt.hotelbooking.notification.model.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByReservationIdAndSubject(UUID reservationId, String subject);

    boolean existsByReferenceIdAndSubject(UUID referenceId, String subject);

    List<Notification> findByStatusAndAttemptsLessThan(NotificationStatus status, int attempts);

    List<Notification> findAllByOrderByLastAttemptAtDesc();

    Page<Notification> findByHotelIdAndNotificationTypeOrderByCreatedAtDesc(
            String hotelId,
            NotificationType notificationType,
            Pageable pageable);
}
