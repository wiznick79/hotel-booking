package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.DiscountCodeRequest;
import pt.hotelbooking.booking.model.entity.DiscountCode;
import pt.hotelbooking.booking.service.DiscountCodeService;
import pt.hotelbooking.booking.config.HotelScopeAuthorization;

import java.util.UUID;

@RestController
@RequestMapping("/api/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOOKING_POLICY_MANAGE')")
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountCode create(@Valid @RequestBody DiscountCodeRequest request, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return discountCodeService.create(request);
    }

    @PatchMapping("/{id}")
    public DiscountCode update(
            @PathVariable UUID id,
            @Valid @RequestBody DiscountCodeRequest request,
            Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, discountCodeService.findHotelId(id));
        return discountCodeService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, discountCodeService.findHotelId(id));
        discountCodeService.deactivate(id);
    }
}
