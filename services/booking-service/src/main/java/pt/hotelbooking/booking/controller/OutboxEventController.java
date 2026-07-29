package pt.hotelbooking.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.OutboxEventResponse;
import pt.hotelbooking.booking.service.OutboxEventService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/outbox-events")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('OUTBOX_MANAGE')")
public class OutboxEventController {

    private final OutboxEventService outboxEventService;

    @GetMapping("/failed")
    public List<OutboxEventResponse> findFailedEvents() {
        return outboxEventService.findFailedEvents();
    }

    @PostMapping("/{id}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OutboxEventResponse replay(@PathVariable UUID id) {
        return outboxEventService.replay(id);
    }
}
