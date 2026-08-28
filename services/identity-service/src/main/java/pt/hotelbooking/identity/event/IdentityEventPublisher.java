package pt.hotelbooking.identity.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.identity.model.entity.IdentityOutboxEvent;
import pt.hotelbooking.identity.repository.IdentityOutboxEventRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityEventPublisher {

    private final IdentityOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${identity-events.topic:identity-events}")
    private String topic;

    @Value("${identity.outbox.maximum-attempts:8}")
    private int maximumAttempts;

    @Value("${identity.outbox.initial-retry-delay-seconds:30}")
    private long initialRetryDelaySeconds;

    @Value("${identity.outbox.maximum-retry-delay-seconds:3600}")
    private long maximumRetryDelaySeconds;

    @Transactional
    public void publishCustomerRegistration(CustomerRegistrationRequestedEvent event, Long userId) {
        try {
            outboxEventRepository.save(new IdentityOutboxEvent(
                    "CustomerRegistrationRequested",
                    userId,
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize customer-registration event.", exception);
        }
    }

    @Scheduled(fixedDelayString = "${identity.outbox.dispatch-delay-ms:5000}")
    public void dispatchPendingEvents() {
        List<IdentityOutboxEvent> events = outboxEventRepository
                .findTop50ByPublishedAtIsNullAndFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                        Instant.now());

        for (IdentityOutboxEvent event : events) {
            try {
                kafkaTemplate.send(MessageBuilder.withPayload(event.getPayload())
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
                        .setHeader("eventType", event.getEventType())
                        .build()).get();
                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception exception) {
                log.warn("Could not deliver identity outbox event {}", event.getId(), exception);
                int nextAttempt = event.getAttempts() + 1;
                long multiplier = 1L << Math.min(nextAttempt - 1, 20);
                long delay = Math.min(initialRetryDelaySeconds * multiplier, maximumRetryDelaySeconds);
                event.markAttempted(abbreviate(exception.getMessage()),
                        Instant.now().plus(Duration.ofSeconds(delay)), maximumAttempts);
                outboxEventRepository.save(event);
            }
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "Unexpected identity event delivery failure.";
        }

        return message.length() <= 2_000 ? message : message.substring(0, 2_000);
    }
}
