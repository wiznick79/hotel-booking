package pt.hotelbooking.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
public class GuestAccessService {

    private final long gracePeriodDays;

    private final SecureRandom secureRandom = new SecureRandom();

    public GuestAccessService(
            @Value("${booking.guest-access.grace-period-days:7}") long gracePeriodDays) {
        if (gracePeriodDays < 0) {
            throw new IllegalArgumentException("Guest access grace period cannot be negative.");
        }

        this.gracePeriodDays = gracePeriodDays;
    }

    public GuestAccessToken createToken(LocalDate checkOutDate) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        Instant expiresAt = checkOutDate.plusDays(gracePeriodDays)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        return new GuestAccessToken(
                rawToken,
                hash(rawToken),
                expiresAt);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public record GuestAccessToken(String rawToken, String hash, Instant expiresAt) {
    }
}
