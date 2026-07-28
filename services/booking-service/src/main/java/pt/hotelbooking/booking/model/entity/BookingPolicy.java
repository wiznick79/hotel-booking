package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.UUID;

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

    public BookingPolicy(String hotelId, boolean payLaterAllowed,
                         int maxUnconfirmedBookings, Duration holdDuration) {
        this.hotelId = hotelId;
        this.payLaterAllowed = payLaterAllowed;
        this.maxUnconfirmedBookings = maxUnconfirmedBookings;
        this.holdDurationMinutes = holdDuration.toMinutes();
        this.cancellationDeadlineDays = 2;
    }

    public Duration getHoldDuration() {
        return Duration.ofMinutes(holdDurationMinutes);
    }

    public void update(boolean payLaterAllowed, int maxUnconfirmedBookings, Duration holdDuration,
                       int cancellationDeadlineDays) {
        this.payLaterAllowed = payLaterAllowed;
        this.maxUnconfirmedBookings = maxUnconfirmedBookings;
        this.holdDurationMinutes = holdDuration.toMinutes();
        this.cancellationDeadlineDays = cancellationDeadlineDays;
    }

    public int getCancellationDeadlineDays() {
        return cancellationDeadlineDays;
    }
}
