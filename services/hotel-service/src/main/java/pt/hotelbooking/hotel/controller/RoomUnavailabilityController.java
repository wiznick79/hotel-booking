package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import pt.hotelbooking.hotel.model.dto.RoomUnavailabilityRequest;
import pt.hotelbooking.hotel.model.dto.RoomUnavailabilityResponse;
import pt.hotelbooking.hotel.service.RoomUnavailabilityService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/room-unavailabilities")
@RequiredArgsConstructor
public class RoomUnavailabilityController {

    private final RoomUnavailabilityService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomUnavailabilityResponse create(@Valid @RequestBody RoomUnavailabilityRequest request,
                                             Authentication authentication) {
        String actor = authentication == null ? "unknown" : authentication.getName();
        return service.create(request, actor);
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAuthority('ROOM_READ')")
    public List<RoomUnavailabilityResponse> findByRoom(@PathVariable UUID roomId) {
        return service.findByRoom(roomId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        service.delete(id, authentication.getName());
    }

    @GetMapping("/room/{roomId}/availability")
    public boolean isAvailable(@PathVariable UUID roomId,
                               @RequestParam LocalDate fromDate,
                               @RequestParam LocalDate toDate) {
        return service.isAvailable(roomId, fromDate, toDate);
    }
}
