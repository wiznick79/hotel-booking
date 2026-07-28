package pt.hotelbooking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void sendGuestAccessLink(String recipient, String reservationId, String rawToken) {
        log.info("Development email to {} for reservation {}: /api/reservations/guest/{}",
                recipient, reservationId, rawToken);
    }
}
