package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rate_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RatePeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal normalNightlyPrice;

    private BigDecimal weekendNightlyPrice;

    private String name;

    public RatePeriod(RoomType roomType, LocalDate startDate, LocalDate endDate,
                      BigDecimal normalNightlyPrice, BigDecimal weekendNightlyPrice, String name) {
        this.roomType = roomType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.normalNightlyPrice = normalNightlyPrice;
        this.weekendNightlyPrice = weekendNightlyPrice;
        this.name = name;
    }
}
