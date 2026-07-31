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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pricing_rule_room_type_prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingRuleRoomTypePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pricing_rule_id", nullable = false)
    private PricingRule pricingRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    private BigDecimal normalNightlyPrice;
    private BigDecimal weekendNightlyPrice;

    public PricingRuleRoomTypePrice(RoomType roomType, BigDecimal normalNightlyPrice,
                                    BigDecimal weekendNightlyPrice) {
        this.roomType = roomType;
        this.normalNightlyPrice = normalNightlyPrice;
        this.weekendNightlyPrice = weekendNightlyPrice;
    }

    void assignPricingRule(PricingRule pricingRule) {
        this.pricingRule = pricingRule;
    }
}
