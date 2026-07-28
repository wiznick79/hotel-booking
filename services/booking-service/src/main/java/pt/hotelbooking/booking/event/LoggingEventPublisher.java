package pt.hotelbooking.booking.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.hotelbooking.booking.model.entity.OutboxEvent;
import pt.hotelbooking.booking.repository.OutboxEventRepository;

import java.util.List;

@Component
@Profile({"dev", "test"})
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    private final OutboxEventRepository outboxRepository;

    private final ObjectMapper objectMapper;

    public LoggingEventPublisher(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(ReservationCreatedEvent event) {
        try {
            outboxRepository.save(new OutboxEvent(
                    "ReservationCreated",
                    event.reservationId(),
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize reservation event.", exception);
        }
    }

    @Scheduled(fixedDelayString = "${booking.outbox.dispatch-delay-ms:5000}")
    public void dispatchPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAt();

        for (OutboxEvent event : events) {
            try {
                log.info("Development outbox event {}: {}", event.getEventType(), event.getPayload());
                event.markPublished();
            } catch (RuntimeException exception) {
                event.markAttempted();
            }

            outboxRepository.save(event);
        }
    }
}
