package pt.hotelbooking.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.hotel.model.entity.WebsiteMedia;
import pt.hotelbooking.hotel.model.entity.WebsiteMediaUsage;

import java.util.List;
import java.util.UUID;

public interface WebsiteMediaRepository extends JpaRepository<WebsiteMedia, UUID> {
    List<WebsiteMedia> findByHotelIdAndErasedFalseOrderByUsageAscSortOrderAscCreatedAtAsc(UUID hotelId);
    List<WebsiteMedia> findByHotelIdAndUsageAndErasedFalse(UUID hotelId, WebsiteMediaUsage usage);
}
