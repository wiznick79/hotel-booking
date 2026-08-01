package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.RoomType;

import java.util.UUID;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByErasedFalse();
}
