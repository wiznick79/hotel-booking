package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.hotelbooking.hotel.model.entity.RoomUnavailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomUnavailabilityRepository extends JpaRepository<RoomUnavailability, UUID> {

    @Query("""
            select count(u) > 0 from RoomUnavailability u
            where u.room.id = :roomId
            and u.fromDate < :toDate
            and u.toDate > :fromDate
            """)
    boolean overlaps(@Param("roomId") UUID roomId,
                     @Param("fromDate") LocalDate fromDate,
                     @Param("toDate") LocalDate toDate);

    List<RoomUnavailability> findByRoomIdOrderByFromDate(UUID roomId);
}
