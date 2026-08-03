package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    public void addRoomType(String roomTypeId) {
        items.add(new ReservationItem(this, roomTypeId));
    }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.HELD) {
            throw new IllegalStateException("Only pending or held reservations can be confirmed.");
        }

        status = ReservationStatus.CONFIRMED;
        holdUntil = null;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in.");
        }

        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out.");
        }

        status = ReservationStatus.CHECKED_OUT;
        revokeGuestAccess();
    }

    public void markNoShow() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be marked as no-show.");
        }

        status = ReservationStatus.NO_SHOW;
        revokeGuestAccess();
    }

    public void expireHold() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.CANCELLED;
            holdUntil = null;
            revokeGuestAccess();
        }
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

    public void updateDetails(String guestName, String guestPhone, String guestEmail,
                              int guestCount, LocalDate checkInDate, LocalDate checkOutDate,
                              String notes) {
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.guestCount = guestCount;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.notes = notes;
    }

    public void replaceRoomTypes(List<String> roomTypeIds) {
        items.clear();
        roomTypeIds.forEach(this::addRoomType);
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

    public void assignRoom(UUID itemId, String roomId) {
        ReservationItem item = items.stream()
                .filter(reservationItem -> reservationItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RoomReassignmentException("Reservation item does not belong to this reservation."));

        item.assignRoom(roomId);
    }

    public void assignFirstUnassignedRoomOfType(String roomTypeId, String roomId) {
        ReservationItem item = items.stream()
                .filter(reservationItem -> roomTypeId.equals(reservationItem.getRoomTypeId()))
                .filter(reservationItem -> reservationItem.getRoomId() == null)
                .findFirst()
                .orElseThrow(() -> new RoomReassignmentException("No unassigned reservation item exists for this room type."));

        item.assignRoom(roomId);
    }
}
