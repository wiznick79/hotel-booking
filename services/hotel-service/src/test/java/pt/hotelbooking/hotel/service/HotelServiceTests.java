package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.model.dto.HotelRequest;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.repository.HotelRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelServiceTests {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService service;

    @Test
    void defaultsHotelLanguageToEnglish() {
        HotelRequest request = new HotelRequest(
                "Hotel", null, "Address", "City", "Portugal", null);
        when(hotelRepository.save(org.mockito.ArgumentMatchers.any(Hotel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertThat(response.defaultLanguage()).isEqualTo("en");
    }

    @Test
    void deactivatesExistingHotel() {
        UUID id = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        when(hotelRepository.findById(id)).thenReturn(Optional.of(hotel));

        service.deactivate(id);

        assertThat(hotel.isActive()).isFalse();
    }

    @Test
    void rejectsMissingHotel() {
        UUID id = UUID.randomUUID();
        when(hotelRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(HotelNotFoundException.class);
    }
}
