package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.hotelbooking.booking.model.entity.DiscountCode;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {

    Optional<DiscountCode> findByHotelIdAndCodeIgnoreCaseAndErasedFalse(String hotelId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DiscountCode d where d.hotelId = :hotelId and upper(d.code) = upper(:code) and d.erased = false")
    Optional<DiscountCode> findForUpdate(@Param("hotelId") String hotelId, @Param("code") String code);

    Optional<DiscountCode> findByHotelIdAndCodeIgnoreCase(String hotelId, String code);

    List<DiscountCode> findByHotelIdAndErasedFalseOrderByCodeAsc(String hotelId);
}
