package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;
import pt.hotelbooking.hotel.model.dto.PricingRuleRequest;
import pt.hotelbooking.hotel.model.dto.PricingRuleResponse;
import pt.hotelbooking.hotel.model.dto.PricingRuleRoomTypePriceRequest;
import pt.hotelbooking.hotel.model.dto.RateQuoteResponse;
import pt.hotelbooking.hotel.model.entity.PricingRule;
import pt.hotelbooking.hotel.model.entity.PricingRuleRoomTypePrice;
import pt.hotelbooking.hotel.model.entity.PricingRuleType;
import pt.hotelbooking.hotel.model.entity.RoomType;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.PricingRuleRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Transactional
    public PricingRuleResponse create(PricingRuleRequest request) {
        validatePeriod(request);

        var hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));
        Set<UUID> seenRoomTypeIds = new HashSet<>();
        PricingRule pricingRule = new PricingRule(hotel, request.name().trim(), request.ruleType(),
                request.recurringStartMonth(), request.recurringStartDay(),
                request.recurringEndMonth(), request.recurringEndDay(), request.startDate(),
                request.endDate(), request.priority());

        for (PricingRuleRoomTypePriceRequest requestedPrice : request.roomTypePrices()) {
            if (!seenRoomTypeIds.add(requestedPrice.roomTypeId())) {
                throw new IllegalArgumentException("A room type can only have one price in a pricing rule.");
            }

            RoomType roomType = roomTypeRepository.findById(requestedPrice.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(requestedPrice.roomTypeId()));
            if (!roomType.getHotel().getId().equals(hotel.getId())) {
                throw new IllegalArgumentException("Every room type price must belong to the selected hotel.");
            }

            pricingRule.addRoomTypePrice(new PricingRuleRoomTypePrice(roomType,
                    requestedPrice.normalNightlyPrice(), requestedPrice.weekendNightlyPrice()));
        }

        return PricingRuleResponse.from(pricingRuleRepository.save(pricingRule));
    }

    @Transactional
    public PricingRuleResponse update(UUID id, PricingRuleRequest request) {
        validatePeriod(request);
        PricingRule pricingRule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        if (!pricingRule.getHotel().getId().equals(request.hotelId())) {
            throw new IllegalArgumentException("A pricing rule cannot be moved to another hotel.");
        }
        pricingRule.update(request.name().trim(), request.ruleType(), request.recurringStartMonth(),
                request.recurringStartDay(), request.recurringEndMonth(), request.recurringEndDay(),
                request.startDate(), request.endDate(), request.priority());
        pricingRule.replaceRoomTypePrices(resolvePrices(request, pricingRule.getHotel().getId()));
        return PricingRuleResponse.from(pricingRule);
    }

    @Transactional
    public void deactivate(UUID id) {
        pricingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id))
                .deactivate();
    }

    @Transactional(readOnly = true)
    public UUID findHotelId(UUID id) {
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id))
                .getHotel().getId();
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> findByHotel(UUID hotelId) {
        return pricingRuleRepository.findByHotelIdAndErasedFalseOrderByPriorityDesc(hotelId).stream()
                .map(PricingRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RateQuoteResponse quote(UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        List<PricingRule> pricingRules = pricingRuleRepository
                .findByHotelIdAndActiveTrueAndErasedFalseOrderByPriorityDesc(roomType.getHotel().getId());
        pricingRules = pricingRules.stream()
                .sorted(Comparator.comparing(PricingRule::getPriority).reversed()
                        .thenComparing(rule -> rule.getRuleType() == PricingRuleType.DATE_OVERRIDE ? 0 : 1))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate night = checkInDate; night.isBefore(checkOutDate); night = night.plusDays(1)) {
            total = total.add(resolveNightlyPrice(roomType, pricingRules, night));
        }

        return new RateQuoteResponse(roomTypeId, checkInDate, checkOutDate, total);
    }

    private BigDecimal resolveNightlyPrice(RoomType roomType, List<PricingRule> pricingRules, LocalDate night) {
        for (PricingRule rule : pricingRules) {
            if (!matches(rule, night)) {
                continue;
            }

            for (PricingRuleRoomTypePrice price : rule.getRoomTypePrices()) {
                if (Objects.equals(price.getRoomType().getId(), roomType.getId())) {
                    return weekendPriceOrNormalPrice(price, night);
                }
            }
        }

        return roomType.getBasePrice();
    }

    private boolean matches(PricingRule pricingRule, LocalDate date) {
        if (pricingRule.getRuleType() == PricingRuleType.DATE_OVERRIDE) {
            return !date.isBefore(pricingRule.getStartDate()) && !date.isAfter(pricingRule.getEndDate());
        }

        MonthDay currentDay = MonthDay.from(date);
        MonthDay startDay = MonthDay.of(pricingRule.getRecurringStartMonth(), pricingRule.getRecurringStartDay());
        MonthDay endDay = MonthDay.of(pricingRule.getRecurringEndMonth(), pricingRule.getRecurringEndDay());
        if (startDay.compareTo(endDay) <= 0) {
            return currentDay.compareTo(startDay) >= 0 && currentDay.compareTo(endDay) <= 0;
        }
        return currentDay.compareTo(startDay) >= 0 || currentDay.compareTo(endDay) <= 0;
    }

    private BigDecimal weekendPriceOrNormalPrice(PricingRuleRoomTypePrice price, LocalDate date) {
        boolean weekendNight = date.getDayOfWeek() == DayOfWeek.FRIDAY
                || date.getDayOfWeek() == DayOfWeek.SATURDAY;
        if (weekendNight && price.getWeekendNightlyPrice() != null) {
            return price.getWeekendNightlyPrice();
        }
        return price.getNormalNightlyPrice();
    }

    private void validatePeriod(PricingRuleRequest request) {
        if (request.ruleType() == PricingRuleType.DATE_OVERRIDE) {
            if (request.startDate() == null || request.endDate() == null) {
                throw new IllegalArgumentException("A date override requires a start date and an end date.");
            }
            if (request.endDate().isBefore(request.startDate())) {
                throw new IllegalArgumentException("End date must be on or after start date.");
            }
            return;
        }

        if (request.recurringStartMonth() == null || request.recurringStartDay() == null
                || request.recurringEndMonth() == null || request.recurringEndDay() == null) {
            throw new IllegalArgumentException("A recurring season requires a start and end month/day.");
        }
        try {
            MonthDay.of(request.recurringStartMonth(), request.recurringStartDay());
            MonthDay.of(request.recurringEndMonth(), request.recurringEndDay());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Recurring season dates must be valid month/day values.");
        }
    }

    private List<PricingRuleRoomTypePrice> resolvePrices(PricingRuleRequest request, UUID hotelId) {
        Set<UUID> seenRoomTypeIds = new HashSet<>();
        return request.roomTypePrices().stream().map(requestedPrice -> {
            if (!seenRoomTypeIds.add(requestedPrice.roomTypeId())) {
                throw new IllegalArgumentException("A room type can only have one price in a pricing rule.");
            }
            RoomType roomType = roomTypeRepository.findById(requestedPrice.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(requestedPrice.roomTypeId()));
            if (!roomType.getHotel().getId().equals(hotelId)) {
                throw new IllegalArgumentException("Every room type price must belong to the selected hotel.");
            }
            return new PricingRuleRoomTypePrice(roomType, requestedPrice.normalNightlyPrice(),
                    requestedPrice.weekendNightlyPrice());
        }).toList();
    }
}
