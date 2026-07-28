package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingPolicyRequest(@NotBlank String hotelId,
                                   boolean payLaterAllowed,
                                   @Min(0) int maxUnconfirmedBookings,
                                   @Min(1) long holdDurationMinutes,
                                   @Min(0) int cancellationDeadlineDays) {
}
