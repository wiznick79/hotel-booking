package pt.hotelbooking.booking.model.dto;

import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationItem;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record ReservationResponse(UUID id, String hotelId, String guestName, String guestPhone,
                                  String guestEmail, int guestCount, LocalDate checkInDate,
                                  LocalDate checkOutDate, String notes, ReservationStatus status,
                                  List<String> roomIds, BigDecimal totalPrice, String currency,
                                  PaymentMode paymentMode, boolean manualConfirmationRequired,
                                  Instant holdUntil, String discountCode, BigDecimal discountAmount) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getHotelId(), reservation.getGuestName(),
                reservation.getGuestPhone(), reservation.getGuestEmail(), reservation.getGuestCount(),
                reservation.getCheckInDate(), reservation.getCheckOutDate(), reservation.getNotes(),
                reservation.getStatus(), reservation.getItems().stream().map(ReservationItem::getRoomId).toList(),
                reservation.getTotalPrice(), reservation.getCurrency(), reservation.getPaymentMode(),
                reservation.isManualConfirmationRequired(), reservation.getHoldUntil(),
                reservation.getDiscountCode(), reservation.getDiscountAmount());
    }
}
