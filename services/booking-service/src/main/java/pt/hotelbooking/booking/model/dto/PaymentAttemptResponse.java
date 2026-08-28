package pt.hotelbooking.booking.model.dto;

import java.util.UUID;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.PaymentMethod;

public record PaymentAttemptResponse(UUID id, PaymentMethod paymentMethod,
                                     PaymentAttemptStatus status, String redirectUrl) {
    public static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getPaymentMethod(),
                attempt.getStatus(), attempt.getRedirectUrl());
    }
}
