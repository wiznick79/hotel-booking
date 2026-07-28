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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepo;
    private final HotelRepository hotelRepo;

    @Transactional
    public RoomTypeResponse create(RoomTypeRequest request, String language) {
        Hotel hotel = hotelRepo.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));
        RoomType roomType = new RoomType(hotel, request.maximumOccupancy(), request.basePrice());
        request.translations().forEach((code, translation) -> roomType.addTranslation(
                new RoomTypeTranslation(code, translation.name(), translation.description())));
        return RoomTypeResponse.from(roomTypeRepo.save(roomType), language);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> findAll(String language) {
        return roomTypeRepo.findAll().stream().map(type -> RoomTypeResponse.from(type, language)).toList();
    }
}
