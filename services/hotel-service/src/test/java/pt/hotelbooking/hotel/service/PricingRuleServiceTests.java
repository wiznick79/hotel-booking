package pt.hotelbooking.hotel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.hotel.model.entity.Hotel;
import pt.hotelbooking.hotel.model.entity.PricingRule;
import pt.hotelbooking.hotel.model.entity.PricingRuleRoomTypePrice;
import pt.hotelbooking.hotel.model.entity.PricingRuleType;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.PricingRuleRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceTests {

    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private PricingRuleService service;

    @Test
    void quoteUsesFridayAndSaturdayPricesFromRecurringSeason() {
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, null, null, null, "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        PricingRule rule = new PricingRule(hotel, "Summer", PricingRuleType.RECURRING_SEASON,
                7, 1, 8, 31, null, null, 1);
        rule.addRoomTypePrice(new PricingRuleRoomTypePrice(roomType, BigDecimal.valueOf(120),
                BigDecimal.valueOf(150)));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(pricingRuleRepository.findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(hotel.getId()))
                .thenReturn(List.of(rule));

        var quote = service.quote(roomTypeId, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 5));

        assertThat(quote.totalPrice()).isEqualByComparingTo("420");
    }

    @Test
    void dateOverrideWinsOverRecurringSeasonAtTheSamePriority() {
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, null, null, null, "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        PricingRule override = new PricingRule(hotel, "Easter", PricingRuleType.DATE_OVERRIDE,
                null, null, null, null, LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 5), 10);
        override.addRoomTypePrice(new PricingRuleRoomTypePrice(roomType, BigDecimal.valueOf(250), null));
        PricingRule recurring = new PricingRule(hotel, "Spring", PricingRuleType.RECURRING_SEASON,
                4, 1, 4, 30, null, null, 10);
        recurring.addRoomTypePrice(new PricingRuleRoomTypePrice(roomType, BigDecimal.valueOf(150), null));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(pricingRuleRepository.findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(hotel.getId()))
                .thenReturn(List.of(override, recurring));

        var quote = service.quote(roomTypeId, LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 4));

        assertThat(quote.totalPrice()).isEqualByComparingTo("250");
    }

    @Test
    void recurringSeasonCanCrossNewYear() {
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, null, null, null, "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        PricingRule rule = new PricingRule(hotel, "Christmas", PricingRuleType.RECURRING_SEASON,
                12, 20, 1, 5, null, null, 1);
        rule.addRoomTypePrice(new PricingRuleRoomTypePrice(roomType, BigDecimal.valueOf(200), null));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(pricingRuleRepository.findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(hotel.getId()))
                .thenReturn(List.of(rule));

        var quote = service.quote(roomTypeId, LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2));

        assertThat(quote.totalPrice()).isEqualByComparingTo("600");
    }

    @Test
    void quoteFallsBackToRoomTypeBasePrice() {
        UUID roomTypeId = UUID.randomUUID();
        Hotel hotel = new Hotel("Hotel", null, null, null, null, "en");
        RoomType roomType = new RoomType(hotel, 2, BigDecimal.valueOf(100));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(pricingRuleRepository.findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(hotel.getId()))
                .thenReturn(List.of());

        var quote = service.quote(roomTypeId, LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 4));

        assertThat(quote.totalPrice()).isEqualByComparingTo("100");
    }

    @Test
    void rejectsInvalidDateOverride() {
        var request = new pt.hotelbooking.hotel.model.dto.PricingRuleRequest(UUID.randomUUID(), "Easter",
                PricingRuleType.DATE_OVERRIDE, null, null, null, null, LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 4, 5), 1, List.of(new pt.hotelbooking.hotel.model.dto.PricingRuleRoomTypePriceRequest(
                UUID.randomUUID(), BigDecimal.TEN, null)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be on or after start date.");
    }
}
