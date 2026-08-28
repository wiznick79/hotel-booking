package pt.hotelbooking.booking.model.dto;

import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.model.entity.PaymentMode;

public record PaymentMethodAvailabilityResponse(PaymentMethod paymentMethod,
                                                PaymentMode paymentMode) {
}
