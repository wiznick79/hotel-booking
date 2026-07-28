package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByCreatedAt();
}
