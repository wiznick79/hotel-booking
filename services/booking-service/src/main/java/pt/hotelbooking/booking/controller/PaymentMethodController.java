package pt.hotelbooking.booking.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.PaymentMethodAvailabilityResponse;
import pt.hotelbooking.booking.service.BookingPolicyService;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final BookingPolicyService bookingPolicyService;

    @GetMapping
    public List<PaymentMethodAvailabilityResponse> findAvailableMethods(@RequestParam String hotelId) {
        return bookingPolicyService.findAvailablePaymentMethods(hotelId);
    }
}
