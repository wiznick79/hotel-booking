package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.OutboxEventResponse;
import pt.hotelbooking.booking.model.entity.OutboxEvent;
import pt.hotelbooking.booking.repository.OutboxEventRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> findFailedEvents() {
        return outboxEventRepository.findByFailedAtIsNotNullOrderByFailedAtDesc().stream()
                .map(OutboxEventResponse::from)
                .toList();
    }

    @Transactional
    public OutboxEventResponse replay(UUID id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found."));

        if (event.getFailedAt() == null) {
            throw new IllegalStateException("Only failed outbox events can be replayed.");
        }

        event.requeue();
        return OutboxEventResponse.from(event);
    }
}
