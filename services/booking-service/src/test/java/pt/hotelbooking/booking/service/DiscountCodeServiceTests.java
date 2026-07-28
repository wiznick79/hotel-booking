package pt.hotelbooking.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.booking.model.entity.DiscountCode;
import pt.hotelbooking.booking.repository.DiscountCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscountCodeServiceTests {

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @InjectMocks
    private DiscountCodeService service;

    @Test
    void appliesPercentageDiscount() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "SUMMER", BigDecimal.TEN, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null);
        when(discountCodeRepository.findByHotelIdAndCodeIgnoreCase("hotel-1", "summer"))
                .thenReturn(Optional.of(code));

        DiscountCodeService.DiscountResult result = service.apply(
                "hotel-1", "summer", BigDecimal.valueOf(200), LocalDate.of(2026, 7, 1));

        assertThat(result.amount()).isEqualByComparingTo("20.00");
        assertThat(result.total()).isEqualByComparingTo("180.00");
    }

    @Test
    void rejectsExpiredCode() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "SUMMER", BigDecimal.TEN, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1), null);
        when(discountCodeRepository.findByHotelIdAndCodeIgnoreCase("hotel-1", "SUMMER"))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.apply(
                "hotel-1", "SUMMER", BigDecimal.valueOf(200), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount code is expired or unavailable.");
    }

    @Test
    void deactivatesExistingCode() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "SUMMER", BigDecimal.TEN, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null);
        when(discountCodeRepository.findById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(code));

        service.deactivate(java.util.UUID.randomUUID());

        assertThat(code.isValidOn(LocalDate.of(2026, 7, 1))).isFalse();
        verify(discountCodeRepository).findById(org.mockito.ArgumentMatchers.any());
    }
}
