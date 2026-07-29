package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.BookingPolicyRequest;
import pt.hotelbooking.booking.model.entity.BookingPolicy;
import pt.hotelbooking.booking.service.BookingPolicyService;
import pt.hotelbooking.booking.config.HotelScopeAuthorization;

@RestController
@RequestMapping("/api/booking-policies")
@RequiredArgsConstructor
public class BookingPolicyController {

    private final BookingPolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAuthority('BOOKING_POLICY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingPolicy configure(
            @Valid @RequestBody BookingPolicyRequest request,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return policyService.configure(request);
    }
}
