package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.List;

@RestController
@RequestMapping("/api/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISCOUNT_CODE_MANAGE')")
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @GetMapping
    public List<DiscountCode> findByHotel(@RequestParam String hotelId, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return discountCodeService.findByHotel(hotelId);
    }

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

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID id, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, discountCodeService.findHotelId(id));
        discountCodeService.activate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, discountCodeService.findHotelId(id));
        discountCodeService.erase(id);
    }
}
