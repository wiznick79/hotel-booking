package pt.hotelbooking.booking.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

public record ReservationRequest(@NotBlank String hotelId, @NotBlank String guestName,
                                  @NotBlank String guestPhone, String guestEmail, @Min(1) int guestCount,
                                  @NotNull @Future LocalDate checkInDate, @NotNull @Future LocalDate checkOutDate,
                                  String notes, @NotEmpty List<@NotBlank String> roomTypeIds,
                                  PaymentMode paymentMode, PaymentMethod paymentMethod, String discountCode,
                                  @AssertTrue(message = "You must accept the privacy notice to make a booking.")
                                  boolean privacyNoticeAccepted) { }
