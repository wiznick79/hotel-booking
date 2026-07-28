package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountCodeRequest(
        @NotBlank String hotelId,
        @NotBlank String code,
        BigDecimal percentage,
        BigDecimal fixedAmount,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil,
        Integer maximumUses) {

    @AssertTrue(message = "Exactly one discount value must be provided.")
    public boolean hasSingleDiscountValue() {
        return (percentage != null) ^ (fixedAmount != null);
    }
}
