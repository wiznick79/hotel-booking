package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.dto.RoomAssignmentRequest;
import pt.hotelbooking.booking.model.dto.ReservationModificationRequest;
import pt.hotelbooking.booking.service.ReservationService;
import pt.hotelbooking.booking.config.HotelScopeAuthorization;
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

    @PutMapping("/{id}/customer")
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse modify(@PathVariable UUID id,
                                      @Valid @RequestBody ReservationModificationRequest request,
                                      Authentication authentication) {
        return reservationService.modify(id, request, authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse findById(@PathVariable UUID id, Authentication authentication) {
        ReservationResponse reservation = reservationService.findById(id);

        if (hasAuthority(authentication, "RESERVATION_READ")) {
            HotelScopeAuthorization.requireAccess(authentication, reservation.hotelId());
            return reservation;
        }

        List<ReservationResponse> customerReservations = reservationService
                .findMyReservations(authentication.getName());
        boolean belongsToCustomer = customerReservations.stream()
                .anyMatch(customerReservation -> customerReservation.id().equals(id));

        if (!belongsToCustomer) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only access your own reservations.");
        }

        return reservation;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReservationResponse> findByHotelAndDateRange(
            @RequestParam String hotelId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return reservationService.findByHotelAndDateRange(hotelId, from, to);
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAuthority('RESERVATION_READ')")
    public long countPendingConfirmations(@RequestParam String hotelId,
                                          Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return reservationService.countPendingConfirmations(hotelId);
    }

    @GetMapping("/availability")
    public List<pt.hotelbooking.booking.model.dto.AvailabilitySearchResponse> searchAvailability(
            @RequestParam String hotelId,
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate,
            @RequestParam int guestCount) {
        return reservationService.searchAvailability(hotelId, checkInDate, checkOutDate, guestCount);
    }

    @GetMapping("/room/{roomId}/affected")
    @PreAuthorize("hasAuthority('RESERVATION_READ')")
    public List<ReservationResponse> findAffectedByRoomAndDateRange(
            @PathVariable String roomId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam String hotelId,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return reservationService.findAffectedByRoomAndDateRange(roomId, from, to);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, Authentication authentication) {
        ReservationResponse reservation = reservationService.findById(id);

        if (hasAuthority(authentication, "RESERVATION_MANAGE")) {
            HotelScopeAuthorization.requireAccess(authentication, reservation.hotelId());
            reservationService.cancel(id);
            return;
        }

        reservationService.cancelByCustomer(id, authentication.getName());
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse confirm(@PathVariable UUID id, Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.confirm(id, authentication.getName());
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse checkIn(@PathVariable UUID id, Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.checkIn(id, authentication.getName());
    }

    @PatchMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse checkOut(@PathVariable UUID id, Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.checkOut(id, authentication.getName());
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse markNoShow(@PathVariable UUID id, Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.markNoShow(id, authentication.getName());
    }

    @PatchMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse placeHold(@PathVariable UUID id,
                                         @RequestParam Instant holdUntil,
                                         Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.placeHold(id, holdUntil);
    }

    @PatchMapping("/{id}/items/{itemId}/room")
    @PreAuthorize("hasAuthority('RESERVATION_MANAGE')")
    public ReservationResponse assignRoom(@PathVariable UUID id,
                                          @PathVariable UUID itemId,
                                          @Valid @RequestBody RoomAssignmentRequest request,
                                            Authentication authentication) {
        requireStaffHotelAccess(id, authentication);
        return reservationService.assignRoom(id, itemId, request, authentication.getName());
    }

    private void requireStaffHotelAccess(UUID reservationId, Authentication authentication) {
        ReservationResponse reservation = reservationService.findById(reservationId);
        HotelScopeAuthorization.requireAccess(authentication, reservation.hotelId());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
