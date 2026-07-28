package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTests {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private RoomTypeService service;

    @Test
    void createsRoomTypeWithTranslations() {
        UUID hotelId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.save(any(RoomType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomTypeRequest request = new RoomTypeRequest(
                hotelId,
                2,
                BigDecimal.valueOf(100),
                Map.of("en", new RoomTypeRequest.TranslationRequest("Double", "Double room")));

        var response = service.create(request, "en");

        assertThat(response.name()).isEqualTo("Double");
        assertThat(response.description()).isEqualTo("Double room");
        assertThat(response.maximumOccupancy()).isEqualTo(2);
    }

    @Test
    void rejectsRoomTypeForMissingHotel() {
        UUID hotelId = UUID.randomUUID();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        RoomTypeRequest request = new RoomTypeRequest(
                hotelId,
                2,
                BigDecimal.valueOf(100),
                Map.of("en", new RoomTypeRequest.TranslationRequest("Double", "Double room")));

        assertThatThrownBy(() -> service.create(request, "en"))
                .isInstanceOf(HotelNotFoundException.class);
    }
}
