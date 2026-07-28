package pt.hotelbooking.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import pt.hotelbooking.booking.model.entity.OutboxEvent;
import pt.hotelbooking.booking.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingEventPublisherTests {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private LoggingEventPublisher publisher;

    @Test
    void storesReservationEventInOutbox() {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                UUID.randomUUID(), "hotel-1", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12), BigDecimal.valueOf(100), "EUR");

        publisher.publish(event);

        verify(outboxRepository).save(any(OutboxEvent.class));
    }
}
