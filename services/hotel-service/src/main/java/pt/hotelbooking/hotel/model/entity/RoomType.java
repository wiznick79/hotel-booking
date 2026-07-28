package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    private int maximumOccupancy;

    private BigDecimal basePrice;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomTypeTranslation> translations = new ArrayList<>();

    public RoomType(Hotel hotel, int maximumOccupancy, BigDecimal basePrice) {
        this.hotel = hotel;
        this.maximumOccupancy = maximumOccupancy;
        this.basePrice = basePrice;
    }

    public void addTranslation(RoomTypeTranslation translation) {
        translations.add(translation);
        translation.assignRoomType(this);
    }
}
