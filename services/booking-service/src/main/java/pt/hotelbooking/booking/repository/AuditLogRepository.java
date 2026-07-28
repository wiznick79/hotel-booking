package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.AuditLog;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
