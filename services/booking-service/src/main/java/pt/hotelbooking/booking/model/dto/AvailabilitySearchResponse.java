package pt.hotelbooking.booking.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailabilitySearchResponse(
        UUID roomTypeId,
        String name,
        int maximumOccupancy,
        BigDecimal totalPrice,
        String currency) {
}
