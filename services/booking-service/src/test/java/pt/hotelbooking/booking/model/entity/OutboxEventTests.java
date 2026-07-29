package pt.hotelbooking.booking.model.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTests {

    @Test
    void marksEventAsPublished() {
        OutboxEvent event = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{}");

        event.markPublished();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }

    @Test
    void tracksFailedDeliveryAttempt() {
        OutboxEvent event = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{}");

        Instant nextAttempt = Instant.now().plusSeconds(30);
        event.markAttempted("notification-service unavailable", nextAttempt, 3);

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(nextAttempt);
        assertThat(event.getFailedAt()).isNull();
        assertThat(event.getLastError()).isEqualTo("notification-service unavailable");
    }

    @Test
    void marksEventAsFailedAfterMaximumAttempts() {
        OutboxEvent event = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{}");

        event.markAttempted("failure", Instant.now().plusSeconds(30), 1);

        assertThat(event.getFailedAt()).isNotNull();
    }

    @Test
    void requeuesFailedEventForManualReplay() {
        OutboxEvent event = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{}");
        event.markAttempted("failure", Instant.now().plusSeconds(30), 1);

        event.requeue();

        assertThat(event.getFailedAt()).isNull();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getNextAttemptAt()).isBeforeOrEqualTo(Instant.now());
    }
}
