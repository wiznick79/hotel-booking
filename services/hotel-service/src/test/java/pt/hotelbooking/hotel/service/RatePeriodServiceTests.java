package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.hotel.model.entity.RatePeriod;
import pt.hotelbooking.hotel.repository.RatePeriodRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatePeriodServiceTests {

    @Mock
    private RatePeriodRepository ratePeriodRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RatePeriodService service;

    @Test
    void quoteUsesWeekendRateForFridayAndSaturday() {
        UUID roomTypeId = UUID.randomUUID();
        RatePeriod ratePeriod = new RatePeriod(
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 6),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(150),
                "Summer");
        when(ratePeriodRepository.findByRoomTypeId(roomTypeId)).thenReturn(List.of(ratePeriod));

        var quote = service.quote(
                roomTypeId,
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 6));

        assertThat(quote.totalPrice()).isEqualByComparingTo("400");
    }

    @Test
    void fallsBackToNormalRateWhenWeekendRateIsMissing() {
        UUID roomTypeId = UUID.randomUUID();
        RatePeriod ratePeriod = new RatePeriod(
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 6),
                BigDecimal.valueOf(100),
                null,
                "Summer");
        when(ratePeriodRepository.findByRoomTypeId(roomTypeId)).thenReturn(List.of(ratePeriod));

        var quote = service.quote(
                roomTypeId,
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 6));

        assertThat(quote.totalPrice()).isEqualByComparingTo("300");
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThatThrownBy(() -> service.quote(
                UUID.randomUUID(),
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be after start date.");
    }

    @Test
    void rejectsDatesWithoutConfiguredRate() {
        UUID roomTypeId = UUID.randomUUID();
        when(ratePeriodRepository.findByRoomTypeId(roomTypeId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.quote(
                roomTypeId,
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No rate configured for 2026-07-03");
    }

    @Test
    void rejectsRatePeriodWithInvalidDates() {
        var request = new pt.hotelbooking.hotel.model.dto.RatePeriodRequest(
                UUID.randomUUID(),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 10),
                BigDecimal.valueOf(100),
                null,
                "Invalid");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be after start date.");
    }
}
