package pt.hotelbooking.hotel.model;

import org.junit.jupiter.api.Test;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.Room;
import pt.hotelbooking.hotel.model.entity.RoomStatus;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.model.entity.RoomTypeTranslation;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HotelDomainTests {

    @Test
    void baseEntityCanBeDeactivatedAndErased() {
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");

        hotel.deactivate();
        assertThat(hotel.isActive()).isFalse();

        hotel.activate();
        assertThat(hotel.isActive()).isTrue();

        hotel.erase();
        assertThat(hotel.isErased()).isTrue();
        assertThat(hotel.isActive()).isFalse();
    }

    @Test
    void roomStartsAvailableAndCanChangeStatus() {
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        Room room = new Room(hotel, roomType, "101", 1);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);

        room.changeStatus(RoomStatus.MAINTENANCE);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void roomTypeMaintainsItsTranslations() {
        Hotel hotel = new Hotel("Hotel", null, "Address", "City", "Portugal", "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        RoomTypeTranslation translation = new RoomTypeTranslation("pt-PT", "Duplo", "Quarto duplo");

        roomType.addTranslation(translation);

        assertThat(roomType.getTranslations()).containsExactly(translation);
        assertThat(translation.getLanguageCode()).isEqualTo("pt-PT");
    }
}
