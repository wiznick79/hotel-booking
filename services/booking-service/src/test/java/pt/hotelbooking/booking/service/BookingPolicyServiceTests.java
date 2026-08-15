package pt.hotelbooking.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.booking.model.dto.BookingPolicyRequest;
import pt.hotelbooking.booking.repository.BookingPolicyRepository;
import pt.hotelbooking.booking.payment.PaymentProviderRegistry;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPolicyServiceTests {

    @Mock
    private BookingPolicyRepository policyRepository;

    @Mock
    private PaymentProviderRegistry paymentProviderRegistry;

    @InjectMocks
    private BookingPolicyService service;

    @Test
    void storesCancellationDeadlineWhenCreatingPolicy() {
        when(policyRepository.findByHotelId("hotel-1")).thenReturn(Optional.empty());
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var policy = service.configure(new BookingPolicyRequest(
                "hotel-1", true, 5, 60, 7));

        assertThat(policy.getCancellationDeadlineDays()).isEqualTo(7);
        assertThat(policy.getHoldDuration()).isEqualTo(Duration.ofMinutes(60));
    }
}
