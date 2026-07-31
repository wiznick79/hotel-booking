package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.dto.RoomTypeResponse;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.model.entity.RoomTypeTranslation;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;
import pt.hotelbooking.hotel.repository.RoomRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepo;
    private final HotelRepository hotelRepo;
    private final RoomRepository roomRepository;

    @Transactional
    public RoomTypeResponse create(RoomTypeRequest request, String language) {
        Hotel hotel = hotelRepo.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));

        List<Map.Entry<String, RoomTypeRequest.TranslationRequest>> translations = request.translations().entrySet()
                .stream()
                .filter(entry -> entry.getValue().name() != null && !entry.getValue().name().isBlank())
                .toList();

        if (translations.isEmpty()) {
            throw new IllegalArgumentException("At least one room type translation name is required.");
        }

        RoomType roomType = new RoomType(hotel, request.maximumOccupancy(), request.basePrice());
        translations.forEach(entry -> roomType.addTranslation(new RoomTypeTranslation(
                entry.getKey(),
                entry.getValue().name().trim(),
                entry.getValue().description())));
        return RoomTypeResponse.from(roomTypeRepo.save(roomType), language);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> findAll(String language) {
        return roomTypeRepo.findAll().stream().map(type -> RoomTypeResponse.from(type, language)).toList();
    }

    @Transactional
    public RoomTypeResponse update(UUID id, RoomTypeRequest request, String language) {
        RoomType roomType = roomTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found: " + id));
        List<Map.Entry<String, RoomTypeRequest.TranslationRequest>> translations = request.translations().entrySet().stream()
                .filter(entry -> entry.getValue().name() != null && !entry.getValue().name().isBlank()).toList();
        if (translations.isEmpty()) throw new IllegalArgumentException("At least one room type translation name is required.");
        roomType.update(request.maximumOccupancy(), request.basePrice());
        roomType.replaceTranslations(translations.stream().map(entry -> new RoomTypeTranslation(entry.getKey(), entry.getValue().name().trim(), entry.getValue().description())).toList());
        return RoomTypeResponse.from(roomType, language);
    }

    @Transactional
    public void deactivate(UUID id) {
        RoomType roomType = findEntity(id);
        if (roomRepository.existsByRoomTypeIdAndActiveTrueAndErasedFalse(id)) {
            throw new IllegalStateException("A room type with active rooms cannot be deactivated.");
        }
        roomType.deactivate();
    }

    @Transactional(readOnly = true)
    public UUID findHotelId(UUID id) {
        return findEntity(id).getHotel().getId();
    }

    private RoomType findEntity(UUID id) {
        return roomTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found: " + id));
    }
}
