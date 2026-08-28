package pt.hotelbooking.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import pt.hotelbooking.booking.payment.PaymentProvider;
import pt.hotelbooking.booking.payment.PaymentProviderRegistry;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.payment.PaymentWebhook;
import pt.hotelbooking.booking.payment.PaymentWebhookResult;
import pt.hotelbooking.booking.repository.PaymentAttemptRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private PaymentProviderRegistry paymentProviderRegistry;

    @Mock
    private PaymentAttemptInitializationService paymentAttemptInitializationService;

    @Mock
    private PaymentProvider stripePaymentProvider;

    @Test
    void correlatesEarlyWebhookUsingProviderMetadataPaymentAttemptId() {
        UUID paymentAttemptId = UUID.randomUUID();
        Reservation reservation = reservation();
        PaymentAttempt attempt = new PaymentAttempt(
                reservation,
                PaymentProviderType.STRIPE,
                PaymentMethod.MULTIBANCO,
                "pending-" + paymentAttemptId,
                null,
                null);
        PaymentWebhookResult webhookResult = new PaymentWebhookResult(
                null,
                "pi_123",
                paymentAttemptId,
                PaymentWebhookResult.PaymentOutcome.SUCCEEDED,
                null);

        when(paymentProviderRegistry.providerFor(PaymentProviderType.STRIPE))
                .thenReturn(stripePaymentProvider);
        when(stripePaymentProvider.verifyWebhook(org.mockito.ArgumentMatchers.any()))
                .thenReturn(webhookResult);
        when(paymentAttemptRepository.findByProviderAndProviderPaymentIntentId(
                PaymentProviderType.STRIPE,
                "pi_123"))
                .thenReturn(Optional.empty());
        when(paymentAttemptRepository.findById(paymentAttemptId)).thenReturn(Optional.of(attempt));

        PaymentService paymentService = new PaymentService(
                paymentAttemptRepository,
                paymentProviderRegistry,
                paymentAttemptInitializationService);

        paymentService.processWebhook(
                PaymentProviderType.STRIPE,
                new PaymentWebhook("signature", "payload", Map.of()));

        assertThat(attempt.getProviderPaymentIntentId()).isEqualTo("pi_123");
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    private Reservation reservation() {
        Reservation reservation = new Reservation(
                "hotel-id",
                "Guest",
                "+351911111111",
                "guest@example.test",
                1,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 21),
                null);
        reservation.applyPriceSnapshot(BigDecimal.valueOf(50), "EUR");
        reservation.configurePayment(PaymentMode.PAY_NOW, PaymentMethod.MULTIBANCO, false);

        return reservation;
    }
}
