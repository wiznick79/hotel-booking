package pt.hotelbooking.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pt.hotelbooking.notification.service.NotificationProcessor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventKafkaListener {

    private final ObjectMapper objectMapper;
    private final NotificationProcessor notificationProcessor;

    @KafkaListener(topics = "${booking-events.topic}")
    public void consume(String payload, @Header("eventType") String eventType) throws Exception {
        if ("ReservationCreated".equals(eventType)) {
            notificationProcessor.processReservationCreated(
                    objectMapper.readValue(payload, ReservationCreatedEvent.class));
            return;
        }

        notificationProcessor.processReservationEvent(
                objectMapper.readValue(payload, ReservationNotificationEvent.class));
    }
}
