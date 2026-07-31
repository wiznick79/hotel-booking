package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;
import pt.hotelbooking.hotel.model.dto.RoomRequest;
import pt.hotelbooking.hotel.model.dto.RoomResponse;
import pt.hotelbooking.hotel.model.dto.RoomUpdateRequest;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.Room;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepo;
    private final HotelRepository hotelRepo;
    private final RoomTypeRepository roomTypeRepo;

    @Transactional(readOnly = true)
    public RoomResponse findById(UUID id) {
        return roomRepo.findById(id)
                .map(RoomResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Hotel hotel = hotelRepo.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));
        RoomType roomType = roomTypeRepo.findById(request.roomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException(request.roomTypeId()));
        Room room = new Room(hotel, roomType, request.roomNumber(), request.floor());
        return RoomResponse.from(roomRepo.save(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> findAll() {
        return roomRepo.findAll().stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse update(UUID id, RoomUpdateRequest request) {
        Room room = roomRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
        RoomType roomType = roomTypeRepo.findById(request.roomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException(request.roomTypeId()));
        if (!roomType.getHotel().getId().equals(room.getHotel().getId())) {
            throw new IllegalArgumentException("Room type must belong to the room's hotel.");
        }
        room.update(roomType, request.roomNumber(), request.floor(), request.status());
        return RoomResponse.from(room);
    }
}
