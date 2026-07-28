package pt.hotelbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOOKING_POLICY_MANAGE')")
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountCode create(@Valid @RequestBody DiscountCodeRequest request) {
        return discountCodeService.create(request);
    }

    @PatchMapping("/{id}")
    public DiscountCode update(
            @PathVariable UUID id,
            @Valid @RequestBody DiscountCodeRequest request) {
        return discountCodeService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        discountCodeService.deactivate(id);
    }
}
