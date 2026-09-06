package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountCodeRequest(
        @NotBlank String hotelId,
        @NotBlank String code,
        @DecimalMin(value = "0.01") BigDecimal percentage,
        @DecimalMin(value = "0.01") BigDecimal fixedAmount,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil,
        @Positive Integer maximumUses) {

    @AssertTrue(message = "Exactly one discount value must be provided.")
    public boolean hasSingleDiscountValue() {
        return (percentage != null) ^ (fixedAmount != null);
    }
}
