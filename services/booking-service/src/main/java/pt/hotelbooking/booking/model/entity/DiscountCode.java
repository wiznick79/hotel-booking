package pt.hotelbooking.booking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "discount_codes")
@Getter
@NoArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String hotelId;

    private String code;

    private BigDecimal percentage;

    private BigDecimal fixedAmount;

    private LocalDate validFrom;

    private LocalDate validUntil;

    private Integer maximumUses;

    private int usedCount;

    private boolean active = true;

    private boolean erased = false;

    public DiscountCode(
            String hotelId,
            String code,
            BigDecimal percentage,
            BigDecimal fixedAmount,
            LocalDate validFrom,
            LocalDate validUntil,
            Integer maximumUses) {
        this.hotelId = hotelId;
        this.code = code;
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.maximumUses = maximumUses;
    }

    public boolean isValidOn(LocalDate date) {
        return active
                && !date.isBefore(validFrom)
                && !date.isAfter(validUntil)
                && (maximumUses == null || usedCount < maximumUses);
    }

    public void registerUse() {
        usedCount++;
    }

    public void update(
            BigDecimal percentage,
            BigDecimal fixedAmount,
            LocalDate validFrom,
            LocalDate validUntil,
            Integer maximumUses) {
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.maximumUses = maximumUses;
    }

    public void deactivate() {
        active = false;
    }

    public void activate() {
        if (!erased) {
            active = true;
        }
    }

    public void erase() {
        erased = true;
        active = false;
    }
}
