package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RoomTypeRequest(@NotNull UUID hotelId, @Min(1) int maximumOccupancy,
                              @NotNull @DecimalMin("0.0") BigDecimal basePrice,
                              @NotNull Map<String, TranslationRequest> translations) {

    public record TranslationRequest(String name, String description) {
    }
}
