package pt.hotelbooking.booking.model.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

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

        event.markAttempted();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }
}
