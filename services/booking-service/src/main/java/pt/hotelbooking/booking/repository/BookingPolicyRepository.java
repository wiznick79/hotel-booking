package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.BookingPolicy;

import java.util.Optional;
import java.util.UUID;

public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, UUID> {
    Optional<BookingPolicy> findByHotelId(String hotelId);
}
