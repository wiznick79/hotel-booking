package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;
import pt.hotelbooking.hotel.model.dto.RatePeriodRequest;
import pt.hotelbooking.hotel.model.dto.RatePeriodResponse;
import pt.hotelbooking.hotel.model.entity.RatePeriod;
import pt.hotelbooking.hotel.repository.RatePeriodRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;

import java.util.List;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import pt.hotelbooking.hotel.model.dto.RateQuoteResponse;

@Service
@RequiredArgsConstructor
public class RatePeriodService {

    private final RatePeriodRepository ratePeriodRepo;
    private final RoomTypeRepository roomTypeRepo;

    @Transactional
    public RatePeriodResponse create(RatePeriodRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }

        if (ratePeriodRepo.existsByRoomTypeIdAndStartDateLessThanAndEndDateGreaterThan(
                request.roomTypeId(), request.endDate(), request.startDate())) {
            throw new IllegalArgumentException("The rate period overlaps an existing rate period.");
        }

        var roomType = roomTypeRepo.findById(request.roomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException(request.roomTypeId()));
        RatePeriod ratePeriod = new RatePeriod(roomType, request.startDate(), request.endDate(),
                request.normalNightlyPrice(), request.weekendNightlyPrice(), request.name());
        return RatePeriodResponse.from(ratePeriodRepo.save(ratePeriod));
    }

    @Transactional(readOnly = true)
    public List<RatePeriodResponse> findByRoomType(java.util.UUID roomTypeId) {
        return ratePeriodRepo.findByRoomTypeId(roomTypeId).stream()
                .map(RatePeriodResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UUID findHotelIdByRoomType(UUID roomTypeId) {
        return roomTypeRepo.findById(roomTypeId)
                .map(roomType -> roomType.getHotel().getId())
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
    }

    @Transactional(readOnly = true)
    public RateQuoteResponse quote(UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }

        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = checkInDate;

        while (date.isBefore(checkOutDate)) {
            LocalDate currentDate = date;
            RatePeriod period = ratePeriodRepo.findByRoomTypeId(roomTypeId).stream()
                    .filter(rate -> !currentDate.isBefore(rate.getStartDate())
                            && currentDate.isBefore(rate.getEndDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No rate configured for " + currentDate));
            BigDecimal price = currentDate.getDayOfWeek() == DayOfWeek.FRIDAY
                    || currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    ? period.getWeekendNightlyPrice() : null;
            total = total.add(price == null ? period.getNormalNightlyPrice() : price);
            date = date.plusDays(1);
        }

        return new RateQuoteResponse(roomTypeId, checkInDate, checkOutDate, total);
    }
}
