package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.BookingPolicyRequest;
import pt.hotelbooking.booking.model.entity.BookingPolicy;
import pt.hotelbooking.booking.repository.BookingPolicyRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import pt.hotelbooking.booking.model.dto.PaymentMethodAvailabilityResponse;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.payment.PaymentProviderRegistry;

@Service
@RequiredArgsConstructor
public class BookingPolicyService {

    private final BookingPolicyRepository policyRepo;
    private final PaymentProviderRegistry paymentProviderRegistry;

    @Transactional
    public BookingPolicy configure(BookingPolicyRequest request) {
        BookingPolicy policy = policyRepo.findByHotelId(request.hotelId())
                .orElseGet(() -> new BookingPolicy(request.hotelId(), request.payLaterAllowed(),
                        request.maxUnconfirmedBookings(), Duration.ofMinutes(request.holdDurationMinutes()),
                        request.enabledOnlinePaymentMethods()));
        policy.update(request.payLaterAllowed(), request.maxUnconfirmedBookings(),
                Duration.ofMinutes(request.holdDurationMinutes()), request.cancellationDeadlineDays(),
                request.enabledOnlinePaymentMethods());
        return policyRepo.save(policy);
    }

    @Transactional(readOnly = true)
    public Optional<BookingPolicy> findByHotel(String hotelId) {
        return policyRepo.findByHotelId(hotelId);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodAvailabilityResponse> findAvailablePaymentMethods(String hotelId) {
        List<PaymentMethodAvailabilityResponse> methods = new ArrayList<>();
        BookingPolicy policy = policyRepo.findByHotelId(hotelId).orElse(null);

        if (policy == null || policy.isPayLaterAllowed()) {
            methods.add(new PaymentMethodAvailabilityResponse(
                    PaymentMethod.PAY_AT_RECEPTION,
                    PaymentMode.PAY_AT_RECEPTION));
        }

        if (policy != null) {
            policy.getEnabledOnlinePaymentMethods().stream()
                    .filter(paymentProviderRegistry::hasExactlyOneProviderFor)
                    .sorted()
                    .map(method -> new PaymentMethodAvailabilityResponse(method, PaymentMode.PAY_NOW))
                    .forEach(methods::add);
        }

        return methods;
    }
}
