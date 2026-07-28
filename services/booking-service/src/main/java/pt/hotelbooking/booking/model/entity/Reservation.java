package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.Duration;
import pt.hotelbooking.booking.exception.RoomReassignmentException;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BookingBaseEntity {
    private String hotelId;

    private String customerUsername;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private int guestCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String notes;

    private BigDecimal totalPrice;

    private String currency;

    private String discountCode;

    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode = PaymentMode.PAY_AT_RECEPTION;

    private boolean manualConfirmationRequired;

    private Instant holdUntil;

    private String guestAccessTokenHash;

    private Instant guestAccessTokenExpiresAt;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> items = new ArrayList<>();

    public Reservation(String hotelId, String guestName, String guestPhone, String guestEmail,
                       int guestCount, LocalDate checkInDate, LocalDate checkOutDate, String notes) {
        this.hotelId = hotelId;
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.guestCount = guestCount;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.notes = notes;
    }

    public void assignCustomer(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public void addRoom(String roomId) {
        items.add(new ReservationItem(this, roomId));
    }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
    }

    public void applyPriceSnapshot(BigDecimal totalPrice, String currency) {
        this.totalPrice = totalPrice;
        this.currency = currency;
    }

    public void applyDiscountSnapshot(String discountCode, BigDecimal discountAmount) {
        this.discountCode = discountCode;
        this.discountAmount = discountAmount;
    }

    public void configurePayment(PaymentMode paymentMode, boolean manualConfirmationRequired) {
        this.paymentMode = paymentMode;
        this.manualConfirmationRequired = manualConfirmationRequired;
    }

    public void placeHold(Instant holdUntil) {
        status = ReservationStatus.HELD;
        this.holdUntil = holdUntil;
    }

    public void configureGuestAccess(String tokenHash, Instant expiresAt) {
        this.guestAccessTokenHash = tokenHash;
        this.guestAccessTokenExpiresAt = expiresAt;
    }

    public boolean hasValidGuestAccess(Instant now) {
        return status != ReservationStatus.CANCELLED
                && guestAccessTokenHash != null
                && guestAccessTokenExpiresAt != null
                && guestAccessTokenExpiresAt.isAfter(now);
    }

    public void revokeGuestAccess() {
        guestAccessTokenHash = null;
        guestAccessTokenExpiresAt = null;
    }

    public void reassignRoom(String currentRoomId, String replacementRoomId) {
        ReservationItem item = items.stream()
                .filter(reservationItem -> reservationItem.getRoomId().equals(currentRoomId))
                .findFirst()
                .orElseThrow(() -> new RoomReassignmentException("Room is not assigned to this reservation."));

        item.changeRoom(replacementRoomId);
    }
}
