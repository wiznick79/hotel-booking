package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.RoomType;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(UUID id, UUID hotelId, int maximumOccupancy, BigDecimal basePrice,
                               String language, String name, String description, boolean active) {
    public static RoomTypeResponse from(RoomType roomType, String language) {
        return roomType.getTranslations().stream()
                .filter(t -> t.getLanguageCode().equalsIgnoreCase(language))
                .findFirst()
                .or(() -> roomType.getTranslations().stream()
                        .filter(t -> t.getLanguageCode().equalsIgnoreCase(roomType.getHotel().getDefaultLanguage()))
                        .findFirst())
                .or(() -> roomType.getTranslations().stream().findFirst())
                .map(t -> new RoomTypeResponse(roomType.getId(), roomType.getHotel().getId(),
                        roomType.getMaximumOccupancy(), roomType.getBasePrice(), t.getLanguageCode(),
                        t.getName(), t.getDescription(), roomType.isActive()))
                .orElseThrow();
    }
}
