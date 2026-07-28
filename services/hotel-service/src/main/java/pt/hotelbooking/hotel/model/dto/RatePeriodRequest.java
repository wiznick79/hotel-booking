package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RatePeriodRequest(@NotNull UUID roomTypeId,
                                @NotNull LocalDate startDate,
                                @NotNull LocalDate endDate,
                                @NotNull @DecimalMin("0.0") BigDecimal normalNightlyPrice,
                                @DecimalMin("0.0") BigDecimal weekendNightlyPrice,
                                @NotBlank String name) {
}
