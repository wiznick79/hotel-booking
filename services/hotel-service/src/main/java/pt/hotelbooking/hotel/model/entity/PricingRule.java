package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pricing_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
    private String name;

    @Enumerated(EnumType.STRING)
    private PricingRuleType ruleType;

    private Integer recurringStartMonth;
    private Integer recurringStartDay;
    private Integer recurringEndMonth;
    private Integer recurringEndDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private int priority;

    @OneToMany(mappedBy = "pricingRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PricingRuleRoomTypePrice> roomTypePrices = new ArrayList<>();

    public PricingRule(Hotel hotel, String name, PricingRuleType ruleType,
                       Integer recurringStartMonth, Integer recurringStartDay,
                       Integer recurringEndMonth, Integer recurringEndDay,
                       LocalDate startDate, LocalDate endDate, int priority) {
        this.hotel = hotel;
        this.name = name;
        this.ruleType = ruleType;
        this.recurringStartMonth = recurringStartMonth;
        this.recurringStartDay = recurringStartDay;
        this.recurringEndMonth = recurringEndMonth;
        this.recurringEndDay = recurringEndDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
    }

    public void addRoomTypePrice(PricingRuleRoomTypePrice roomTypePrice) {
        roomTypePrices.add(roomTypePrice);
        roomTypePrice.assignPricingRule(this);
    }

    public void update(String name, PricingRuleType ruleType,
                       Integer recurringStartMonth, Integer recurringStartDay,
                       Integer recurringEndMonth, Integer recurringEndDay,
                       LocalDate startDate, LocalDate endDate, int priority) {
        this.name = name;
        this.ruleType = ruleType;
        this.recurringStartMonth = recurringStartMonth;
        this.recurringStartDay = recurringStartDay;
        this.recurringEndMonth = recurringEndMonth;
        this.recurringEndDay = recurringEndDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
    }

    public void replaceRoomTypePrices(List<PricingRuleRoomTypePrice> prices) {
        roomTypePrices.clear();
        prices.forEach(this::addRoomTypePrice);
    }
}
