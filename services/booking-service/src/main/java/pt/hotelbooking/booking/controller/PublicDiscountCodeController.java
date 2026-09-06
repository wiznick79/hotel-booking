package pt.hotelbooking.booking.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.DiscountCodeValidationResponse;
import pt.hotelbooking.booking.service.DiscountCodeService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/discount-codes")
@RequiredArgsConstructor
public class PublicDiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @GetMapping("/validate")
    public DiscountCodeValidationResponse validate(
            @RequestParam @NotBlank String hotelId,
            @RequestParam @NotBlank String code,
            @RequestParam @NotNull @DecimalMin("0.00") BigDecimal total,
            @RequestParam @NotNull LocalDate stayDate) {
        DiscountCodeService.DiscountResult result = discountCodeService.preview(hotelId, code, total, stayDate);
        return new DiscountCodeValidationResponse(result.code(), total, result.amount(), result.total());
    }
}
