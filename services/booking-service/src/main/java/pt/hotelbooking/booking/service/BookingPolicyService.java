package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.BookingPolicyRequest;
import pt.hotelbooking.booking.model.entity.BookingPolicy;
import pt.hotelbooking.booking.repository.BookingPolicyRepository;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingPolicyService {

    private final BookingPolicyRepository policyRepo;

    @Transactional
    public BookingPolicy configure(BookingPolicyRequest request) {
        BookingPolicy policy = policyRepo.findByHotelId(request.hotelId())
                .orElseGet(() -> new BookingPolicy(request.hotelId(), request.payLaterAllowed(),
                        request.maxUnconfirmedBookings(), Duration.ofMinutes(request.holdDurationMinutes())));
        policy.update(request.payLaterAllowed(), request.maxUnconfirmedBookings(),
                Duration.ofMinutes(request.holdDurationMinutes()), request.cancellationDeadlineDays());
        return policyRepo.save(policy);
    }

    @Transactional(readOnly = true)
    public Optional<BookingPolicy> findByHotel(String hotelId) {
        return policyRepo.findByHotelId(hotelId);
    }
}
