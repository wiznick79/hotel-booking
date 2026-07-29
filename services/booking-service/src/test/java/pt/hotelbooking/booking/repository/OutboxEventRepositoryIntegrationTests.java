package pt.hotelbooking.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pt.hotelbooking.booking.model.entity.OutboxEvent;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OutboxEventRepositoryIntegrationTests {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldReturnOnlyUnpublishedEventsInCreationOrder() {
        OutboxEvent first = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{\"reservationId\":\"1\"}");
        OutboxEvent second = new OutboxEvent(
                "ReservationCreated",
                UUID.randomUUID(),
                "{\"reservationId\":\"2\"}");

        outboxEventRepository.save(first);
        outboxEventRepository.saveAndFlush(second);

        List<OutboxEvent> events = outboxEventRepository
                .findTop50ByPublishedAtIsNullAndFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                        Instant.now());

        assertThat(events).containsExactly(first, second);
    }
}
