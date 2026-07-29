package pt.hotelbooking.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("resilientRateLimiter")
@Primary
public class ResilientRateLimiter implements RateLimiter<RedisRateLimiter.Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientRateLimiter.class);

    private static final int MAX_FALLBACK_BUCKETS = 10_000;

    private static final Duration FALLBACK_BUCKET_IDLE_TIME = Duration.ofMinutes(10);

    private final RedisRateLimiter redisRateLimiter;

    private final Map<String, TokenBucket> fallbackBuckets = new ConcurrentHashMap<>();

    public ResilientRateLimiter(RedisRateLimiter redisRateLimiter) {
        this.redisRateLimiter = redisRateLimiter;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String key) {
        return redisRateLimiter.isAllowed(routeId, key)
                .onErrorResume(exception -> {
                    LOGGER.warn("Redis rate limit check failed for route '{}' and key '{}'. "
                                    + "Using the local emergency limiter.",
                            routeId, key, exception);

                    return Mono.fromSupplier(() -> localResponse(routeId, key));
                });
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return redisRateLimiter.getConfigClass();
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return redisRateLimiter.newConfig();
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return redisRateLimiter.getConfig();
    }

    private Response localResponse(String routeId, String key) {
        RedisRateLimiter.Config configuration = redisRateLimiter.getConfig().get(routeId);

        if (configuration == null) {
            LOGGER.error("No rate-limit configuration exists for route '{}'. Denying request during Redis outage.",
                    routeId);
            return new Response(false, Map.of());
        }

        removeIdleBuckets();

        String bucketKey = routeId + ':' + key;
        TokenBucket bucket = fallbackBuckets.computeIfAbsent(bucketKey,
                ignored -> TokenBucket.full(configuration));

        return new Response(bucket.tryConsume(configuration), Map.of());
    }

    private void removeIdleBuckets() {
        if (fallbackBuckets.size() < MAX_FALLBACK_BUCKETS) {
            return;
        }

        Instant oldestAcceptedAccess = Instant.now().minus(FALLBACK_BUCKET_IDLE_TIME);
        fallbackBuckets.entrySet().removeIf(entry -> entry.getValue().lastAccessed().isBefore(oldestAcceptedAccess));
    }

    private static class TokenBucket {

        private double tokens;

        private Instant lastRefilled;

        private Instant lastAccessed;

        private TokenBucket(double tokens, Instant lastRefilled, Instant lastAccessed) {
            this.tokens = tokens;
            this.lastRefilled = lastRefilled;
            this.lastAccessed = lastAccessed;
        }

        static TokenBucket full(RedisRateLimiter.Config configuration) {
            Instant now = Instant.now();
            return new TokenBucket(configuration.getBurstCapacity(), now, now);
        }

        synchronized boolean tryConsume(RedisRateLimiter.Config configuration) {
            Instant now = Instant.now();
            double elapsedSeconds = Duration.between(lastRefilled, now).toMillis() / 1_000.0;
            tokens = Math.min(configuration.getBurstCapacity(),
                    tokens + elapsedSeconds * configuration.getReplenishRate());
            lastRefilled = now;
            lastAccessed = now;

            if (tokens < configuration.getRequestedTokens()) {
                return false;
            }

            tokens -= configuration.getRequestedTokens();
            return true;
        }

        Instant lastAccessed() {
            return lastAccessed;
        }
    }
}
