package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;
import pt.hotelbooking.hotel.model.dto.RoomRequest;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTests {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomService service;

    @Test
    void rejectsRoomForMissingHotel() {
        UUID hotelId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new RoomRequest(hotelId, roomTypeId, "101", 1)))
                .isInstanceOf(HotelNotFoundException.class);
    }

    @Test
    void rejectsRoomForMissingRoomType() {
        UUID hotelId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new RoomRequest(hotelId, roomTypeId, "101", 1)))
                .isInstanceOf(RoomTypeNotFoundException.class);
    }

    @Test
    void createsRoomWhenHotelAndRoomTypeExist() {
        UUID hotelId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(roomRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new RoomRequest(hotelId, roomTypeId, "101", 1));

        org.assertj.core.api.Assertions.assertThat(response.roomNumber()).isEqualTo("101");
        org.assertj.core.api.Assertions.assertThat(response.status())
                .isEqualTo(pt.hotelbooking.hotel.model.entity.RoomStatus.AVAILABLE);
    }
}
