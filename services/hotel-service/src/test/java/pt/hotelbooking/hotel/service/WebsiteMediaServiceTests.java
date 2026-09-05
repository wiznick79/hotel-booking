package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.hotelbooking.hotel.model.entity.*;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;
import pt.hotelbooking.hotel.repository.WebsiteMediaRepository;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebsiteMediaServiceTests {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    private final WebsiteMediaRepository repository = mock(WebsiteMediaRepository.class);
    private final HotelRepository hotelRepository = mock(HotelRepository.class);
    private final RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
    private final WebsiteMediaStorage storage = mock(WebsiteMediaStorage.class);
    private WebsiteMediaService service;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        reset(repository, hotelRepository, roomTypeRepository, storage);
        service = new WebsiteMediaService(repository, hotelRepository, roomTypeRepository, storage);
        hotel = mock(Hotel.class);
    }

    @Test
    void storesValidatedRoomPhoto() {
        UUID hotelId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        RoomType roomType = mock(RoomType.class);
        when(hotel.getId()).thenReturn(hotelId);
        when(roomType.getHotel()).thenReturn(hotel);
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(hotelId, roomTypeId, WebsiteMediaUsage.ROOM_TYPE_GALLERY, 0, "A bright room",
                new MockMultipartFile("file", "room.png", "image/png", PNG));

        verify(storage).store(matches("[0-9a-f-]+\\.png"), eq(PNG));
        verify(repository).save(argThat(media -> media.getRoomType() == roomType
                && media.getUsage() == WebsiteMediaUsage.ROOM_TYPE_GALLERY));
    }

    @Test
    void rejectsRoomTypeFromAnotherHotel() {
        UUID hotelId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        Hotel otherHotel = mock(Hotel.class);
        RoomType roomType = mock(RoomType.class);
        when(otherHotel.getId()).thenReturn(UUID.randomUUID());
        when(roomType.getHotel()).thenReturn(otherHotel);
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));

        assertThatThrownBy(() -> service.upload(hotelId, roomTypeId, WebsiteMediaUsage.ROOM_TYPE_GALLERY,
                0, null, new MockMultipartFile("file", "room.png", "image/png", PNG)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("does not belong");
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsContentThatOnlyClaimsToBeAnImage() {
        UUID hotelId = UUID.randomUUID();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        assertThatThrownBy(() -> service.upload(hotelId, null, WebsiteMediaUsage.HERO, 0, null,
                new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not a valid image");
        verifyNoInteractions(storage);
    }
}
