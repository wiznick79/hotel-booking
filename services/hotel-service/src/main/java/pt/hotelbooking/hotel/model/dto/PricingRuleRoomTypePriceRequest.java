package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingRuleRoomTypePriceRequest(
        @NotNull UUID roomTypeId,
        @NotNull @DecimalMin("0.0") BigDecimal normalNightlyPrice,
        @DecimalMin("0.0") BigDecimal weekendNightlyPrice) {
}
