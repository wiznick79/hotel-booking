package pt.hotelbooking.booking.model.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPolicyTests {

    @Test
    void storesConfiguredPayLaterPolicy() {
        BookingPolicy policy = new BookingPolicy(
                "hotel-1", true, 10, Duration.ofMinutes(60));

        assertThat(policy.getHotelId()).isEqualTo("hotel-1");
        assertThat(policy.isPayLaterAllowed()).isTrue();
        assertThat(policy.getMaxUnconfirmedBookings()).isEqualTo(10);
        assertThat(policy.getHoldDuration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(policy.getCancellationDeadlineDays()).isEqualTo(2);
    }
}
