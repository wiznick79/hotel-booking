package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pt.hotelbooking.hotel.model.entity.PricingRuleType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PricingRuleRequest(
        @NotNull UUID hotelId,
        @NotBlank String name,
        @NotNull PricingRuleType ruleType,
        Integer recurringStartMonth,
        Integer recurringStartDay,
        Integer recurringEndMonth,
        Integer recurringEndDay,
        LocalDate startDate,
        LocalDate endDate,
        @Min(0) int priority,
        @NotEmpty List<@Valid PricingRuleRoomTypePriceRequest> roomTypePrices) {
}
