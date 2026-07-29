package pt.hotelbooking.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientRateLimiterTests {

    @Test
    void shouldUseLocalTokenBucketWhenRedisFails() {
        RedisRateLimiter redisRateLimiter = mock(RedisRateLimiter.class);
        RedisRateLimiter.Config configuration = new RedisRateLimiter.Config()
                .setReplenishRate(1)
                .setBurstCapacity(2)
                .setRequestedTokens(1);

        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("Redis is unavailable")));
        when(redisRateLimiter.getConfig()).thenReturn(Map.of("booking", configuration));

        ResilientRateLimiter rateLimiter = new ResilientRateLimiter(redisRateLimiter);

        RateLimiter.Response first = rateLimiter.isAllowed("booking", "127.0.0.1").block();
        RateLimiter.Response second = rateLimiter.isAllowed("booking", "127.0.0.1").block();
        RateLimiter.Response third = rateLimiter.isAllowed("booking", "127.0.0.1").block();

        assertThat(first.isAllowed()).isTrue();
        assertThat(second.isAllowed()).isTrue();
        assertThat(third.isAllowed()).isFalse();
    }

    @Test
    void shouldDenyWhenRedisFailsAndRouteHasNoFallbackConfiguration() {
        RedisRateLimiter redisRateLimiter = mock(RedisRateLimiter.class);

        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("Redis is unavailable")));
        when(redisRateLimiter.getConfig()).thenReturn(Map.of());

        ResilientRateLimiter rateLimiter = new ResilientRateLimiter(redisRateLimiter);

        RateLimiter.Response response = rateLimiter.isAllowed("unknown", "127.0.0.1").block();

        assertThat(response.isAllowed()).isFalse();
    }
}
