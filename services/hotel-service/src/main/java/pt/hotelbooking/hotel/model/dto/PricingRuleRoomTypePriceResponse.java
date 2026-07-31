package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.PricingRuleRoomTypePrice;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingRuleRoomTypePriceResponse(UUID roomTypeId, BigDecimal normalNightlyPrice,
                                                BigDecimal weekendNightlyPrice) {
    public static PricingRuleRoomTypePriceResponse from(PricingRuleRoomTypePrice price) {
        return new PricingRuleRoomTypePriceResponse(price.getRoomType().getId(),
                price.getNormalNightlyPrice(), price.getWeekendNightlyPrice());
    }
}
