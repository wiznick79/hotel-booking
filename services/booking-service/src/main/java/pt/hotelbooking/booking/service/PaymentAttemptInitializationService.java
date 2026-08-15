package pt.hotelbooking.booking.service;

import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.payment.PaymentInitiation;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.repository.PaymentAttemptRepository;
import pt.hotelbooking.booking.repository.ReservationRepository;

@Service
@RequiredArgsConstructor
public class PaymentAttemptInitializationService {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt findOrCreatePending(
            UUID reservationId,
            PaymentProviderType provider,
            PaymentMethod paymentMethod) {
        PaymentAttempt existingAttempt = paymentAttemptRepository
                .findByReservationIdAndStatus(reservationId, PaymentAttemptStatus.PENDING)
                .stream()
                .max(Comparator.comparing(PaymentAttempt::getCreatedAt))
                .orElse(null);

        if (existingAttempt != null) {
            return existingAttempt;
        }

        Reservation reservation = reservationRepository.getReferenceById(reservationId);
        PaymentAttempt attempt = new PaymentAttempt(
                reservation,
                provider,
                paymentMethod,
                "pending-" + UUID.randomUUID(),
                null,
                null);

        return paymentAttemptRepository.saveAndFlush(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt completeInitiation(UUID paymentAttemptId, PaymentInitiation initiation) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(paymentAttemptId)
                .orElseThrow(() -> new IllegalStateException("Payment attempt not found."));

        attempt.completeInitiation(
                initiation.providerPaymentId(),
                initiation.providerPaymentIntentId(),
                initiation.redirectUrl());

        return attempt;
    }
}
