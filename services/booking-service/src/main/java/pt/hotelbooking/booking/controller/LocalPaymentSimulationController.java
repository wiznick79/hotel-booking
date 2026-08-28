package pt.hotelbooking.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.service.PaymentService;

@RestController
@RequestMapping("/api/payments/local")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.local-simulation.enabled", havingValue = "true")
public class LocalPaymentSimulationController {
    private final PaymentService paymentService;

    @PostMapping("/{providerPaymentId}/complete")
    public ReservationResponse complete(@PathVariable String providerPaymentId) {
        return paymentService.completeLocalSimulation(providerPaymentId);
    }
}
