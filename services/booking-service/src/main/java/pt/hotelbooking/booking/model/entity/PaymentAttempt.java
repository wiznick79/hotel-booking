package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pt.hotelbooking.booking.payment.PaymentProviderType;

@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt extends BookingBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentAttemptStatus status = PaymentAttemptStatus.PENDING;

    @Column(nullable = false)
    private String providerPaymentId;

    private String providerPaymentIntentId;

    @Column(length = 2000)
    private String redirectUrl;

    private String failureReason;

    private String paymentEntity;

    private String paymentReference;

    @Column(length = 2000)
    private String hostedVoucherUrl;

    private Instant paymentInstructionsExpireAt;

    public PaymentAttempt(Reservation reservation, PaymentProviderType provider,
                          PaymentMethod paymentMethod, String providerPaymentId,
                          String providerPaymentIntentId, String redirectUrl) {
        this.reservation = reservation;
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.providerPaymentId = providerPaymentId;
        this.providerPaymentIntentId = providerPaymentIntentId;
        this.redirectUrl = redirectUrl;
    }

    public boolean markSucceeded() {
        if (status == PaymentAttemptStatus.SUCCEEDED) {
            return false;
        }

        if (status == PaymentAttemptStatus.REFUNDED) {
            return false;
        }

        status = PaymentAttemptStatus.SUCCEEDED;
        failureReason = null;
        return true;
    }

    public void markFailed(String reason) {
        if (status == PaymentAttemptStatus.PENDING) {
            status = PaymentAttemptStatus.FAILED;
            failureReason = reason;
        }
    }

    public void markExpired() {
        if (status == PaymentAttemptStatus.PENDING) {
            status = PaymentAttemptStatus.EXPIRED;
        }
    }

    public void registerProviderPaymentIntentId(String providerPaymentIntentId) {
        if (providerPaymentIntentId != null && !providerPaymentIntentId.isBlank()) {
            this.providerPaymentIntentId = providerPaymentIntentId;
        }
    }

    public void completeInitiation(
            String providerPaymentId,
            String providerPaymentIntentId,
            String redirectUrl) {
        this.providerPaymentId = providerPaymentId;
        this.providerPaymentIntentId = providerPaymentIntentId;
        this.redirectUrl = redirectUrl;
    }

    public void updateInstructions(
            String paymentEntity,
            String paymentReference,
            String hostedVoucherUrl,
            Instant paymentInstructionsExpireAt) {
        this.paymentEntity = paymentEntity;
        this.paymentReference = paymentReference;
        this.hostedVoucherUrl = hostedVoucherUrl;
        this.paymentInstructionsExpireAt = paymentInstructionsExpireAt;
    }

    public boolean belongsTo(PaymentProviderType provider, String providerPaymentId) {
        return this.provider == provider && Objects.equals(this.providerPaymentId, providerPaymentId);
    }
}
