package pt.hotelbooking.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.notification.model.Notification;
import pt.hotelbooking.notification.model.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByReservationIdAndSubject(UUID reservationId, String subject);

    List<Notification> findByStatusAndAttemptsLessThan(NotificationStatus status, int attempts);
}
