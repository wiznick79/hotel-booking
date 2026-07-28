package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.RatePeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RatePeriodResponse(UUID id, UUID roomTypeId, LocalDate startDate,
                                 LocalDate endDate, BigDecimal normalNightlyPrice,
                                 BigDecimal weekendNightlyPrice, String name) {
    public static RatePeriodResponse from(RatePeriod ratePeriod) {
        return new RatePeriodResponse(ratePeriod.getId(), ratePeriod.getRoomType().getId(),
                ratePeriod.getStartDate(), ratePeriod.getEndDate(), ratePeriod.getNormalNightlyPrice(),
                ratePeriod.getWeekendNightlyPrice(),
                ratePeriod.getName());
    }
}
