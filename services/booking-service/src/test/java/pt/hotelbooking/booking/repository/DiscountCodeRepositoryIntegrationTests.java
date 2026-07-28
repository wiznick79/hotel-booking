package pt.hotelbooking.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pt.hotelbooking.booking.model.entity.DiscountCode;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DiscountCodeRepositoryIntegrationTests {

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @Test
    void shouldFindDiscountCodeIgnoringCodeCase() {
        DiscountCode discountCode = new DiscountCode(
                "hotel-1",
                "SUMMER10",
                BigDecimal.TEN,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 1),
                10);

        discountCodeRepository.saveAndFlush(discountCode);

        assertThat(discountCodeRepository.findByHotelIdAndCodeIgnoreCase("hotel-1", "summer10"))
                .containsSame(discountCode);
    }

    @Test
    void shouldPersistRegisteredUsageAndDeactivation() {
        DiscountCode discountCode = new DiscountCode(
                "hotel-1",
                "WELCOME",
                null,
                BigDecimal.valueOf(15),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                1);

        discountCode.registerUse();
        discountCode.deactivate();
        discountCodeRepository.saveAndFlush(discountCode);

        DiscountCode persisted = discountCodeRepository.findById(discountCode.getId()).orElseThrow();

        assertThat(persisted.getUsedCount()).isEqualTo(1);
        assertThat(persisted.isActive()).isFalse();
    }
}
