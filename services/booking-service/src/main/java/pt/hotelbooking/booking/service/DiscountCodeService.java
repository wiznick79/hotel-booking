package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.DiscountCodeRequest;
import pt.hotelbooking.booking.model.entity.DiscountCode;
import pt.hotelbooking.booking.repository.DiscountCodeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;

    @Transactional
    public DiscountCode create(DiscountCodeRequest request) {
        validateRequest(request);

        DiscountCode discountCode = new DiscountCode(
                request.hotelId(),
                request.code().trim().toUpperCase(),
                request.percentage(),
                request.fixedAmount(),
                request.validFrom(),
                request.validUntil(),
                request.maximumUses());

        return discountCodeRepository.save(discountCode);
    }

    @Transactional
    public DiscountCode update(UUID id, DiscountCodeRequest request) {
        validateRequest(request);

        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found."));

        discountCode.update(
                request.percentage(),
                request.fixedAmount(),
                request.validFrom(),
                request.validUntil(),
                request.maximumUses());

        return discountCode;
    }

    @Transactional
    public void deactivate(UUID id) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found."));

        discountCode.deactivate();
    }

    @Transactional(readOnly = true)
    public String findHotelId(UUID id) {
        return discountCodeRepository.findById(id)
                .map(DiscountCode::getHotelId)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found."));
    }

    private void validateRequest(DiscountCodeRequest request) {
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new IllegalArgumentException("Discount validity end must be after its start.");
        }

        if (request.percentage() != null
                && (request.percentage().signum() <= 0
                || request.percentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
    }

    @Transactional
    public DiscountResult apply(String hotelId, String code, BigDecimal total, LocalDate date) {
        if (code == null || code.isBlank()) {
            return new DiscountResult(null, BigDecimal.ZERO, total);
        }

        DiscountCode discountCode = discountCodeRepository
                .findByHotelIdAndCodeIgnoreCase(hotelId, code)
                .orElseThrow(() -> new IllegalArgumentException("Discount code is invalid."));

        if (!discountCode.isValidOn(date)) {
            throw new IllegalArgumentException("Discount code is expired or unavailable.");
        }

        BigDecimal discount = discountCode.getPercentage() != null
                ? total.multiply(discountCode.getPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : discountCode.getFixedAmount().min(total);

        discountCode.registerUse();
        return new DiscountResult(discountCode.getCode(), discount, total.subtract(discount));
    }

    public record DiscountResult(String code, BigDecimal amount, BigDecimal total) {
    }
}
