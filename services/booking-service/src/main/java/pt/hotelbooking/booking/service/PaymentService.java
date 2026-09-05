package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.exception.ReservationNotFoundException;
import pt.hotelbooking.booking.model.dto.PaymentAttemptResponse;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import java.time.Instant;
import pt.hotelbooking.booking.payment.PaymentInitiation;
import pt.hotelbooking.booking.payment.PaymentInitiationRequest;
import pt.hotelbooking.booking.payment.PaymentProvider;
import pt.hotelbooking.booking.payment.PaymentProviderRegistry;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.payment.PaymentWebhook;
import pt.hotelbooking.booking.payment.PaymentWebhookResult;
import pt.hotelbooking.booking.repository.PaymentAttemptRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final PaymentAttemptInitializationService paymentAttemptInitializationService;

    public PaymentAttemptResponse initiate(Reservation reservation) {
        PaymentProvider provider = paymentProviderRegistry.providerFor(reservation.getPaymentMethod());
        PaymentAttempt attempt = paymentAttemptInitializationService.findOrCreatePending(
                reservation.getId(), provider.providerType(), reservation.getPaymentMethod());

        if (attempt.getRedirectUrl() != null) {
            return PaymentAttemptResponse.from(attempt);
        }

        PaymentInitiation initiation = provider.initiate(new PaymentInitiationRequest(
                reservation.getId(), attempt.getId(), reservation.getTotalPrice(), reservation.getCurrency(),
                reservation.getPaymentMethod(), reservation.getGuestEmail(), null, null));

        return PaymentAttemptResponse.from(
                paymentAttemptInitializationService.completeInitiation(attempt.getId(), initiation));
    }

    @Transactional
    public ReservationResponse completeLocalSimulation(String providerPaymentId) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByProviderAndProviderPaymentId(PaymentProviderType.LOCAL_SIMULATION, providerPaymentId)
                .orElseThrow(() -> new ReservationNotFoundException("Payment attempt not found."));

        recordSuccessfulPayment(attempt);

        return ReservationResponse.from(attempt.getReservation(), attempt.getStatus());
    }

    @Transactional
    public void processWebhook(PaymentProviderType providerType, PaymentWebhook webhook) {
        PaymentProvider provider = paymentProviderRegistry.providerFor(providerType);
        PaymentWebhookResult result = provider.verifyWebhook(webhook);

        PaymentAttempt attempt = findPaymentAttempt(providerType, result);
        attempt.registerProviderPaymentIntentId(result.providerPaymentIntentId());

        if (result.instructions() != null) {
            attempt.updateInstructions(
                    result.instructions().entity(),
                    result.instructions().reference(),
                    result.instructions().hostedVoucherUrl(),
                    result.instructions().expiresAt());
        }

        switch (result.outcome()) {
            case SUCCEEDED -> recordSuccessfulPayment(attempt);
            case FAILED, CANCELLED -> attempt.markFailed("The payment provider reported a failed payment.");
            case PENDING -> {
                // A delayed payment method can remain pending after a webhook is received.
            }
        }
    }

    public void expirePendingAttempts(Reservation reservation) {
        paymentAttemptRepository.findByReservationAndStatus(reservation, PaymentAttemptStatus.PENDING)
                .forEach(PaymentAttempt::markExpired);
    }

    private void recordSuccessfulPayment(PaymentAttempt attempt) {
        if (!attempt.markSucceeded()) {
            return;
        }

        Reservation reservation = attempt.getReservation();
        // Provider success records money received even after our local deadline.
        // Never resurrect a cancelled booking: its inventory may already be sold.
        if (reservation.getStatus() == ReservationStatus.HELD
                && reservation.getHoldUntil() != null
                && !reservation.getHoldUntil().isAfter(Instant.now())) {
            reservation.expireHold();
        } else if (reservation.getStatus() == ReservationStatus.PENDING
                || reservation.getStatus() == ReservationStatus.HELD) {
            reservation.confirm();
        }
    }

    private PaymentAttempt findPaymentAttempt(
            PaymentProviderType providerType,
            PaymentWebhookResult result) {
        if (result.providerPaymentId() != null && !result.providerPaymentId().isBlank()) {
            return paymentAttemptRepository
                    .findByProviderAndProviderPaymentId(providerType, result.providerPaymentId())
                    .orElseThrow(() -> new ReservationNotFoundException("Payment attempt not found."));
        }

        if (result.providerPaymentIntentId() != null && !result.providerPaymentIntentId().isBlank()) {
            PaymentAttempt attempt = paymentAttemptRepository
                    .findByProviderAndProviderPaymentIntentId(providerType, result.providerPaymentIntentId())
                    .orElse(null);

            if (attempt != null) {
                return attempt;
            }
        }

        if (result.paymentAttemptId() != null) {
            return paymentAttemptRepository.findById(result.paymentAttemptId())
                    .filter(attempt -> attempt.getProvider() == providerType)
                    .orElseThrow(() -> new ReservationNotFoundException("Payment attempt not found."));
        }

        throw new ReservationNotFoundException("Payment attempt not found.");
    }
}
