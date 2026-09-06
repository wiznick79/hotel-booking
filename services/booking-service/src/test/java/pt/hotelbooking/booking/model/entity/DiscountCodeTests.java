package pt.hotelbooking.booking.model.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountCodeTests {

    @Test
    void validityIncludesTheFinalDate() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "LASTDAY", BigDecimal.TEN, null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 6), null);

        assertThat(code.isValidOn(LocalDate.of(2026, 9, 6))).isTrue();
        assertThat(code.isValidOn(LocalDate.of(2026, 9, 7))).isFalse();
    }

    @Test
    void becomesUnavailableAfterMaximumUses() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "LIMITED", null, BigDecimal.TEN,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 1);

        assertThat(code.isValidOn(LocalDate.of(2026, 7, 1))).isTrue();

        code.registerUse();

        assertThat(code.isValidOn(LocalDate.of(2026, 7, 1))).isFalse();
    }

    @Test
    void deactivatedCodeCannotBeUsed() {
        DiscountCode code = new DiscountCode(
                "hotel-1", "SUMMER", BigDecimal.TEN, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null);

        code.deactivate();

        assertThat(code.isValidOn(LocalDate.of(2026, 7, 1))).isFalse();
    }
}
