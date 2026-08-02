package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.Hotel;
import java.util.UUID;

public record HotelResponse(UUID id, String name, String description, String address,
                            String city, String country, String defaultLanguage,
                            String notificationDisplayName, String notificationFromAddress,
                            String notificationReplyToAddress, boolean active) {
    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(hotel.getId(), hotel.getName(), hotel.getDescription(),
                hotel.getAddress(), hotel.getCity(), hotel.getCountry(), hotel.getDefaultLanguage(),
                hotel.getNotificationDisplayName(), hotel.getNotificationFromAddress(),
                hotel.getNotificationReplyToAddress(), hotel.isActive());
    }
}
