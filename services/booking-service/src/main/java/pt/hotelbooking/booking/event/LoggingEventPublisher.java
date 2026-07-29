package pt.hotelbooking.booking.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.hotelbooking.booking.model.entity.OutboxEvent;
import pt.hotelbooking.booking.repository.OutboxEventRepository;
import pt.hotelbooking.booking.config.OutboxProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

@Component
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    private final OutboxEventRepository outboxRepository;

    private final ObjectMapper objectMapper;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String topic;

    private final GuestAccessTokenCipher guestAccessTokenCipher;

    private final OutboxProperties outboxProperties;

    public LoggingEventPublisher(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @org.springframework.beans.factory.annotation.Value("${booking-events.topic}") String topic,
            GuestAccessTokenCipher guestAccessTokenCipher,
            OutboxProperties outboxProperties) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.guestAccessTokenCipher = guestAccessTokenCipher;
        this.outboxProperties = outboxProperties;
    }

    @Override
    @Transactional
    public void publish(ReservationCreatedEvent event) {
        ReservationCreatedEvent securedEvent = new ReservationCreatedEvent(
                event.reservationId(), event.guestEmail(), event.guestName(),
                event.encryptedGuestAccessToken() == null ? null
                        : guestAccessTokenCipher.encrypt(event.encryptedGuestAccessToken()),
                event.hotelId(), event.checkInDate(), event.checkOutDate(),
                event.totalPrice(), event.currency());
        publishEvent("ReservationCreated", event.reservationId(), securedEvent);
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
        List<OutboxEvent> events = outboxRepository
                .findTop50ByPublishedAtIsNullAndFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                        Instant.now());

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(MessageBuilder.withPayload(event.getPayload())
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
                        .setHeader("eventType", event.getEventType())
                        .setHeader("correlationId", correlationId(event))
                        .build()).get();
                event.markPublished();
                log.info("Published outbox event {} to Kafka topic {}", event.getId(), topic);
            } catch (Exception exception) {
                log.warn("Could not deliver outbox event {}", event.getId(), exception);
                markDeliveryFailure(event, exception);
            }

            outboxRepository.save(event);
        }
    }

    private void markDeliveryFailure(OutboxEvent event, Exception exception) {
        int nextAttemptNumber = event.getAttempts() + 1;
        long multiplier = 1L << Math.min(nextAttemptNumber - 1, 20);
        long retryDelaySeconds = Math.min(
                outboxProperties.initialRetryDelaySeconds() * multiplier,
                outboxProperties.maximumRetryDelaySeconds());
        Instant nextAttemptAt = Instant.now().plus(Duration.ofSeconds(retryDelaySeconds));

        event.markAttempted(
                abbreviateError(exception.getMessage()),
                nextAttemptAt,
                outboxProperties.maximumAttempts());
    }

    private String abbreviateError(String message) {
        if (message == null) {
            return "Unexpected event delivery failure.";
        }

        return message.length() <= 2_000 ? message : message.substring(0, 2_000);
    }

    private String correlationId(OutboxEvent event) {
        String correlationId = MDC.get("correlationId");
        return correlationId == null ? event.getId().toString() : correlationId;
    }
}
