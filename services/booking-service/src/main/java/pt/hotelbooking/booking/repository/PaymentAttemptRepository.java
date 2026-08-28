package pt.hotelbooking.booking.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.payment.PaymentProviderType;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByProviderAndProviderPaymentId(
            PaymentProviderType provider,
            String providerPaymentId);

    Optional<PaymentAttempt> findByProviderAndProviderPaymentIntentId(
            PaymentProviderType provider,
            String providerPaymentIntentId);

    List<PaymentAttempt> findByReservationAndStatus(Reservation reservation, PaymentAttemptStatus status);

    List<PaymentAttempt> findByReservationIdAndStatus(UUID reservationId, PaymentAttemptStatus status);

    Optional<PaymentAttempt> findFirstByReservationOrderByCreatedAtDesc(Reservation reservation);
}
