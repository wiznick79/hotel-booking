package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.PricingRule;

import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    List<PricingRule> findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(UUID hotelId);

    List<PricingRule> findByHotelIdAndErasedFalseOrderByPriorityDesc(UUID hotelId);
}
