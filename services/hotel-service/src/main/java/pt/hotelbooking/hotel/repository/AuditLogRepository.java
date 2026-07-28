package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.AuditLog;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
