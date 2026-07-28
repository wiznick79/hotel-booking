package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.Instant;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class HoldExpirationJob {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${booking.holds.expiration-check-delay-ms:60000}")
    public void expireHolds() {
        reservationService.expireHolds(Instant.now());
    }
}
