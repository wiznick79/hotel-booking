package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.Notification;
import pt.hotelbooking.booking.model.entity.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByStatusAndAttemptsLessThan(
            NotificationStatus status,
            int attempts);
}
