package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.DiscountCode;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {

    Optional<DiscountCode> findByHotelIdAndCodeIgnoreCase(String hotelId, String code);

    List<DiscountCode> findByHotelIdOrderByCodeAsc(String hotelId);
}
