package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.PricingRule;
import pt.hotelbooking.hotel.model.entity.PricingRuleType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PricingRuleResponse(UUID id, UUID hotelId, boolean active, String name, PricingRuleType ruleType,
                                  Integer recurringStartMonth, Integer recurringStartDay,
                                  Integer recurringEndMonth, Integer recurringEndDay,
                                  LocalDate startDate, LocalDate endDate, int priority,
                                  List<PricingRuleRoomTypePriceResponse> roomTypePrices) {
    public static PricingRuleResponse from(PricingRule pricingRule) {
        return new PricingRuleResponse(pricingRule.getId(), pricingRule.getHotel().getId(), pricingRule.isActive(),
                pricingRule.getName(), pricingRule.getRuleType(), pricingRule.getRecurringStartMonth(),
                pricingRule.getRecurringStartDay(), pricingRule.getRecurringEndMonth(),
                pricingRule.getRecurringEndDay(), pricingRule.getStartDate(), pricingRule.getEndDate(),
                pricingRule.getPriority(), pricingRule.getRoomTypePrices().stream()
                        .map(PricingRuleRoomTypePriceResponse::from)
                        .toList());
    }
}
