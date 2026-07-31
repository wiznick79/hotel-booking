package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.hotel.config.HotelScopeAuthorization;
import pt.hotelbooking.hotel.model.dto.PricingRuleRequest;
import pt.hotelbooking.hotel.model.dto.PricingRuleResponse;
import pt.hotelbooking.hotel.model.dto.RateQuoteResponse;
import pt.hotelbooking.hotel.service.PricingRuleService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pricing-rules")
@RequiredArgsConstructor
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    @PreAuthorize("hasAuthority('RATE_PERIOD_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PricingRuleResponse create(@Valid @RequestBody PricingRuleRequest request,
                                      Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return pricingRuleService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RATE_PERIOD_MANAGE')")
    public PricingRuleResponse update(@PathVariable UUID id, @Valid @RequestBody PricingRuleRequest request,
                                      Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return pricingRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RATE_PERIOD_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, pricingRuleService.findHotelId(id));
        pricingRuleService.deactivate(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOTEL_READ')")
    public List<PricingRuleResponse> findByHotel(@RequestParam UUID hotelId,
                                                 Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return pricingRuleService.findByHotel(hotelId);
    }

    @GetMapping("/quote")
    public RateQuoteResponse quote(@RequestParam UUID roomTypeId,
                                   @RequestParam LocalDate checkInDate,
                                   @RequestParam LocalDate checkOutDate) {
        return pricingRuleService.quote(roomTypeId, checkInDate, checkOutDate);
    }
}
