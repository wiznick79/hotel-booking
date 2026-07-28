package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "room_unavailabilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String reason;

    private boolean emergency;

    public RoomUnavailability(Room room, LocalDate fromDate, LocalDate toDate, String reason, boolean emergency) {
        this.room = room;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.emergency = emergency;
    }
}
