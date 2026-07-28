package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import pt.hotelbooking.booking.model.entity.PaymentMode;

public record ReservationRequest(@NotBlank String hotelId, @NotBlank String guestName,
                                  @NotBlank String guestPhone, String guestEmail, @Min(1) int guestCount,
                                  @NotNull @Future LocalDate checkInDate, @NotNull @Future LocalDate checkOutDate,
                                  String notes, @NotEmpty List<@NotBlank String> roomIds,
                                  PaymentMode paymentMode, String discountCode) { }
