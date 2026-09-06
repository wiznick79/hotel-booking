package pt.hotelbooking.booking.model.dto;

import java.math.BigDecimal;

public record DiscountCodeValidationResponse(
        String code,
        BigDecimal originalTotal,
        BigDecimal discountAmount,
        BigDecimal finalTotal) {
}
