package pt.hotelbooking.booking.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GuestAccessServiceTests {

    private final GuestAccessService service = new GuestAccessService(7);

    @Test
    void createsRandomHashableToken() {
        GuestAccessService.GuestAccessToken first = service.createToken(LocalDate.of(2026, 8, 10));
        GuestAccessService.GuestAccessToken second = service.createToken(LocalDate.of(2026, 8, 10));

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(first.hash()).isEqualTo(service.hash(first.rawToken()));
        assertThat(first.expiresAt()).isEqualTo(
                LocalDate.of(2026, 8, 17).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
    }

    @Test
    void tokenExpiresAfterCheckoutGracePeriod() {
        GuestAccessService.GuestAccessToken token = service.createToken(LocalDate.of(2026, 8, 10));

        assertThat(token.expiresAt())
                .isEqualTo(LocalDate.of(2026, 8, 17)
                        .atStartOfDay()
                        .toInstant(java.time.ZoneOffset.UTC));
    }
}
