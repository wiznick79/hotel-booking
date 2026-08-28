package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "booking_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String hotelId;

    private boolean payLaterAllowed;

    private int maxUnconfirmedBookings;

    private long holdDurationMinutes;

    private int cancellationDeadlineDays;

    @ElementCollection(targetClass = PaymentMethod.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_policy_payment_methods", joinColumns = @JoinColumn(name = "booking_policy_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private Set<PaymentMethod> enabledOnlinePaymentMethods = EnumSet.noneOf(PaymentMethod.class);

    public BookingPolicy(String hotelId, boolean payLaterAllowed,
                         int maxUnconfirmedBookings, Duration holdDuration,
                         Set<PaymentMethod> enabledOnlinePaymentMethods) {
        this.hotelId = hotelId;
        this.payLaterAllowed = payLaterAllowed;
        this.maxUnconfirmedBookings = maxUnconfirmedBookings;
        this.holdDurationMinutes = holdDuration.toMinutes();
        this.cancellationDeadlineDays = 2;
        replaceEnabledOnlinePaymentMethods(enabledOnlinePaymentMethods);
    }

    public BookingPolicy(String hotelId, boolean payLaterAllowed,
                         int maxUnconfirmedBookings, Duration holdDuration) {
        this(hotelId, payLaterAllowed, maxUnconfirmedBookings, holdDuration, null);
    }

    public Duration getHoldDuration() {
        return Duration.ofMinutes(holdDurationMinutes);
    }

    public void update(boolean payLaterAllowed, int maxUnconfirmedBookings, Duration holdDuration,
                       int cancellationDeadlineDays, Set<PaymentMethod> enabledOnlinePaymentMethods) {
        this.payLaterAllowed = payLaterAllowed;
        this.maxUnconfirmedBookings = maxUnconfirmedBookings;
        this.holdDurationMinutes = holdDuration.toMinutes();
        this.cancellationDeadlineDays = cancellationDeadlineDays;
        replaceEnabledOnlinePaymentMethods(enabledOnlinePaymentMethods);
    }

    public int getCancellationDeadlineDays() {
        return cancellationDeadlineDays;
    }

    private void replaceEnabledOnlinePaymentMethods(Set<PaymentMethod> paymentMethods) {
        enabledOnlinePaymentMethods = paymentMethods == null || paymentMethods.isEmpty()
                ? EnumSet.noneOf(PaymentMethod.class)
                : EnumSet.copyOf(paymentMethods);

        if (enabledOnlinePaymentMethods.contains(PaymentMethod.PAY_AT_RECEPTION)) {
            throw new IllegalArgumentException("Pay at reception is configured separately from online payment methods.");
        }
    }
}
