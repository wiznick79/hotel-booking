package pt.hotelbooking.hotel.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import lombok.RequiredArgsConstructor;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.model.dto.HotelRequest;
import pt.hotelbooking.hotel.model.dto.HotelResponse;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.repository.HotelRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepo;

    @Transactional(readOnly = true)
    @Cacheable("hotel-list")
    public List<HotelResponse> findAll() {
        return hotelRepo.findAll().stream().map(HotelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotels", key = "#id")
    public HotelResponse findById(UUID id) {
        return HotelResponse.from(findEntity(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "hotel-list", allEntries = true)
    public HotelResponse create(HotelRequest request) {
        Hotel hotel = new Hotel(request.name(), request.description(), request.address(), request.city(),
                request.country(), request.defaultLanguage() == null ? "en" : request.defaultLanguage());
        
        return HotelResponse.from(hotelRepo.save(hotel));
    }

    @Transactional
    @CacheEvict(cacheNames = {"hotel-list", "hotels"}, key = "#id")
    public HotelResponse update(UUID id, HotelRequest request) {
        Hotel hotel = findEntity(id);
        hotel.update(request.name(), request.description(), request.address(), request.city(), request.country(),
                request.defaultLanguage() == null ? "en" : request.defaultLanguage());
        return HotelResponse.from(hotel);
    }

    @Transactional
    @CacheEvict(cacheNames = {"hotel-list", "hotels"}, key = "#id")
    public void deactivate(UUID id) {
        Hotel hotel = findEntity(id);

        hotel.deactivate();
    }

    private Hotel findEntity(UUID id) {
        return hotelRepo.findById(id).orElseThrow(() -> new HotelNotFoundException(id));
    }
}
