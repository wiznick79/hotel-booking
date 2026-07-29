package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.hotel.model.dto.RatePeriodRequest;
import pt.hotelbooking.hotel.model.dto.RatePeriodResponse;
import pt.hotelbooking.hotel.model.dto.RateQuoteResponse;
import pt.hotelbooking.hotel.service.RatePeriodService;
import pt.hotelbooking.hotel.config.HotelScopeAuthorization;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rate-periods")
@RequiredArgsConstructor
public class RatePeriodController {

    private final RatePeriodService ratePeriodService;

    @PostMapping
    @PreAuthorize("hasAuthority('RATE_PERIOD_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RatePeriodResponse create(
            @Valid @RequestBody RatePeriodRequest request,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(
                authentication,
                ratePeriodService.findHotelIdByRoomType(request.roomTypeId()));
        return ratePeriodService.create(request);
    }

    @GetMapping
    public List<RatePeriodResponse> findByRoomType(@RequestParam UUID roomTypeId) {
        return ratePeriodService.findByRoomType(roomTypeId);
    }

    @GetMapping("/quote")
    public RateQuoteResponse quote(@RequestParam UUID roomTypeId,
                                   @RequestParam java.time.LocalDate checkInDate,
                                   @RequestParam java.time.LocalDate checkOutDate) {
        return ratePeriodService.quote(roomTypeId, checkInDate, checkOutDate);
    }
}
