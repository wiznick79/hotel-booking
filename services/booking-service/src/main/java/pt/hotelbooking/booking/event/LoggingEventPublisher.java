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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
@Profile({"dev", "test"})
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    private final OutboxEventRepository outboxRepository;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    private final String notificationServiceUrl;

    private final String notificationServiceToken;

    public LoggingEventPublisher(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${notification-service.url:http://localhost:8084}") String notificationServiceUrl,
            @Value("${notification-service.service-token:change-this-development-token}") String notificationServiceToken) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.notificationServiceUrl = notificationServiceUrl;
        this.notificationServiceToken = notificationServiceToken;
    }

    @Override
    @Transactional
    public void publish(ReservationCreatedEvent event) {
        publishEvent("ReservationCreated", event.reservationId(), event);
    }

    @Override
    @Transactional
    public void publish(ReservationNotificationEvent event) {
        publishEvent(event.eventType(), event.reservationId(), event);
    }

    private void publishEvent(String eventType, java.util.UUID aggregateId, Object event) {
        try {
            outboxRepository.save(new OutboxEvent(
                    eventType,
                    aggregateId,
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
                restClient.mutate().baseUrl(notificationServiceUrl).build()
                        .post()
                        .uri(event.getEventType().equals("ReservationCreated")
                                ? "/internal/events/reservation-created"
                                : "/internal/events/reservation-event")
                        .header("X-Internal-Service-Token", notificationServiceToken)
                        .body(event.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                event.markPublished();
                log.info("Delivered outbox event {} to notification-service", event.getId());
            } catch (RestClientException exception) {
                log.warn("Could not deliver outbox event {}: {}", event.getId(), exception.getMessage());
                event.markAttempted();
            } catch (RuntimeException exception) {
                event.markAttempted();
            }

            outboxRepository.save(event);
        }
    }
}
