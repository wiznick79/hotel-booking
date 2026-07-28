package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.dto.RoomReassignmentRequest;
import pt.hotelbooking.booking.service.ReservationService;
import java.util.UUID;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<ReservationResponse> findMyReservations(Authentication authentication) {
        return reservationService.findMyReservations(authentication.getName());
    }

    @GetMapping("/guest/{token}")
    public ReservationResponse findByGuestAccessToken(@PathVariable String token) {
        return reservationService.findByGuestAccessToken(token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {
        String customerUsername = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : null;

        return reservationService.create(request, customerUsername);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse findById(@PathVariable UUID id) {
        return reservationService.findById(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReservationResponse> findByHotelAndDateRange(
            @RequestParam String hotelId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return reservationService.findByHotelAndDateRange(hotelId, from, to);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        reservationService.cancel(id);
    }

    @PatchMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse placeHold(@PathVariable UUID id,
                                         @RequestParam Instant holdUntil) {
        return reservationService.placeHold(id, holdUntil);
    }

    @PatchMapping("/{id}/rooms")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse reassignRoom(@PathVariable UUID id,
                                            @Valid @RequestBody RoomReassignmentRequest request) {
        return reservationService.reassignRoom(id, request);
    }
}
