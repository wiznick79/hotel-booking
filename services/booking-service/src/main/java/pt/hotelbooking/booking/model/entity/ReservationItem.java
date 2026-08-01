package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationItem extends BookingBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    private String roomTypeId;

    private String roomId;

    public ReservationItem(Reservation reservation, String roomTypeId) {
        this.reservation = reservation;
        this.roomTypeId = roomTypeId;
    }

    public void assignRoom(String roomId) {
        this.roomId = roomId;
    }
}
