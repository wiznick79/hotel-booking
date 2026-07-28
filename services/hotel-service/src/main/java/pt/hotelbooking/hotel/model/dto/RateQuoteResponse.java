package pt.hotelbooking.hotel.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RateQuoteResponse(UUID roomTypeId, LocalDate checkInDate,
                                LocalDate checkOutDate, BigDecimal totalPrice) {
}
