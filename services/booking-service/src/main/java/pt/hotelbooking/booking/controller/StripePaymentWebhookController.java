package pt.hotelbooking.booking.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.payment.PaymentWebhook;
import pt.hotelbooking.booking.service.PaymentService;

@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.stripe.enabled", havingValue = "true")
public class StripePaymentWebhookController {
    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.processWebhook(PaymentProviderType.STRIPE,
                new PaymentWebhook(signature, payload, Map.of()));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
