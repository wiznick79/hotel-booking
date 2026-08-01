package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ReservationModificationRequest(
        @NotBlank String guestName,
        @NotBlank String guestPhone,
        String guestEmail,
        @Min(1) int guestCount,
        @NotNull @Future LocalDate checkInDate,
        @NotNull @Future LocalDate checkOutDate,
        String notes,
        @NotEmpty List<@NotBlank String> roomTypeIds,
        String discountCode) {
}
