package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.RatePeriod;

import java.util.List;
import java.util.UUID;

public interface RatePeriodRepository extends JpaRepository<RatePeriod, UUID> {

    List<RatePeriod> findByRoomTypeId(UUID roomTypeId);
}
