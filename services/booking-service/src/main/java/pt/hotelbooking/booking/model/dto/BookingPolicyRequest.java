package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

public record BookingPolicyRequest(@NotBlank String hotelId,
                                   boolean payLaterAllowed,
                                   @Min(0) int maxUnconfirmedBookings,
                                   @Min(1) long holdDurationMinutes,
                                   @Min(0) int cancellationDeadlineDays,
                                   Set<PaymentMethod> enabledOnlinePaymentMethods) {
    public BookingPolicyRequest(String hotelId, boolean payLaterAllowed,
                                int maxUnconfirmedBookings, long holdDurationMinutes,
                                int cancellationDeadlineDays) {
        this(hotelId, payLaterAllowed, maxUnconfirmedBookings, holdDurationMinutes,
                cancellationDeadlineDays, Set.of());
    }
}
