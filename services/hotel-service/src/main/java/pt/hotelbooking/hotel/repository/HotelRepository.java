package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.Hotel;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {}
