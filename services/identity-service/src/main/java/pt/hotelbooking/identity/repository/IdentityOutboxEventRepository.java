package pt.hotelbooking.identity.repository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.identity.model.entity.IdentityOutboxEvent;

public interface IdentityOutboxEventRepository extends JpaRepository<IdentityOutboxEvent, UUID> {

    List<IdentityOutboxEvent> findTop50ByPublishedAtIsNullAndFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            Instant now);
}
