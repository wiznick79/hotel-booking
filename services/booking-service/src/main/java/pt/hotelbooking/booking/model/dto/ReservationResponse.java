package pt.hotelbooking.booking.model.dto;

import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.model.entity.PaymentAttemptStatus;
import pt.hotelbooking.booking.model.entity.PaymentAttempt;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record ReservationResponse(UUID id, String hotelId, String guestName, String guestPhone,
                                  String guestEmail, int guestCount, LocalDate checkInDate,
                                  LocalDate checkOutDate, String notes, ReservationStatus status,
                                  List<ReservationItemResponse> items, BigDecimal totalPrice, String currency,
                                  PaymentMode paymentMode, PaymentMethod paymentMethod, boolean manualConfirmationRequired,
                                  Instant holdUntil, String discountCode, BigDecimal discountAmount,
                                  PaymentAttemptResponse paymentAttempt, PaymentAttemptStatus paymentStatus,
                                  PaymentInstructionsResponse paymentInstructions) {
    @com.fasterxml.jackson.annotation.JsonProperty("paymentReviewRequired")
    public boolean paymentReviewRequired() {
        return paymentStatus == PaymentAttemptStatus.SUCCEEDED
                && status == ReservationStatus.CANCELLED;
    }

    public ReservationResponse(UUID id, String hotelId, String guestName, String guestPhone,
                               String guestEmail, int guestCount, LocalDate checkInDate,
                               LocalDate checkOutDate, String notes, ReservationStatus status,
                               List<ReservationItemResponse> items, BigDecimal totalPrice, String currency,
                               PaymentMode paymentMode, PaymentMethod paymentMethod,
                               boolean manualConfirmationRequired, Instant holdUntil, String discountCode,
                               BigDecimal discountAmount) {
        this(id, hotelId, guestName, guestPhone, guestEmail, guestCount, checkInDate, checkOutDate,
                notes, status, items, totalPrice, currency, paymentMode, paymentMethod,
                manualConfirmationRequired, holdUntil, discountCode, discountAmount, null, null, null);
    }

    public ReservationResponse(UUID id, String hotelId, String guestName, String guestPhone,
                               String guestEmail, int guestCount, LocalDate checkInDate,
                               LocalDate checkOutDate, String notes, ReservationStatus status,
                               List<ReservationItemResponse> items, BigDecimal totalPrice, String currency,
                               PaymentMode paymentMode, PaymentMethod paymentMethod,
                               boolean manualConfirmationRequired, Instant holdUntil, String discountCode,
                               BigDecimal discountAmount, PaymentAttemptResponse paymentAttempt,
                               PaymentAttemptStatus paymentStatus) {
        this(id, hotelId, guestName, guestPhone, guestEmail, guestCount, checkInDate, checkOutDate,
                notes, status, items, totalPrice, currency, paymentMode, paymentMethod,
                manualConfirmationRequired, holdUntil, discountCode, discountAmount, paymentAttempt,
                paymentStatus, null);
    }

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getHotelId(), reservation.getGuestName(),
                reservation.getGuestPhone(), reservation.getGuestEmail(), reservation.getGuestCount(),
                reservation.getCheckInDate(), reservation.getCheckOutDate(), reservation.getNotes(),
                reservation.getStatus(), reservation.getItems().stream().map(ReservationItemResponse::from).toList(),
                reservation.getTotalPrice(), reservation.getCurrency(), reservation.getPaymentMode(),
                reservation.getPaymentMethod(), reservation.isManualConfirmationRequired(), reservation.getHoldUntil(),
                reservation.getDiscountCode(), reservation.getDiscountAmount(), null, null, null);
    }

    public static ReservationResponse from(Reservation reservation, PaymentAttemptResponse paymentAttempt) {
        ReservationResponse response = from(reservation);
        return new ReservationResponse(response.id(), response.hotelId(), response.guestName(),
                response.guestPhone(), response.guestEmail(), response.guestCount(), response.checkInDate(),
                response.checkOutDate(), response.notes(), response.status(), response.items(),
                response.totalPrice(), response.currency(), response.paymentMode(), response.paymentMethod(),
                response.manualConfirmationRequired(), response.holdUntil(), response.discountCode(),
                response.discountAmount(), paymentAttempt, paymentAttempt.status(), null);
    }

    public static ReservationResponse from(Reservation reservation, PaymentAttemptStatus paymentStatus) {
        ReservationResponse response = from(reservation);
        return new ReservationResponse(response.id(), response.hotelId(), response.guestName(),
                response.guestPhone(), response.guestEmail(), response.guestCount(), response.checkInDate(),
                response.checkOutDate(), response.notes(), response.status(), response.items(),
                response.totalPrice(), response.currency(), response.paymentMode(), response.paymentMethod(),
                response.manualConfirmationRequired(), response.holdUntil(), response.discountCode(),
                response.discountAmount(), null, paymentStatus, null);
    }

    public static ReservationResponse from(Reservation reservation, PaymentAttempt paymentAttempt) {
        ReservationResponse response = from(reservation);
        return new ReservationResponse(response.id(), response.hotelId(), response.guestName(),
                response.guestPhone(), response.guestEmail(), response.guestCount(), response.checkInDate(),
                response.checkOutDate(), response.notes(), response.status(), response.items(),
                response.totalPrice(), response.currency(), response.paymentMode(), response.paymentMethod(),
                response.manualConfirmationRequired(), response.holdUntil(), response.discountCode(),
                response.discountAmount(), null, paymentAttempt.getStatus(),
                PaymentInstructionsResponse.from(paymentAttempt));
    }
}
