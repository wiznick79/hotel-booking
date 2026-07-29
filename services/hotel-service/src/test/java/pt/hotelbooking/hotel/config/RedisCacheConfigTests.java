package pt.hotelbooking.hotel.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheConfigTests {

    @Test
    void shouldIgnoreCacheFailuresSoTheApplicationCanContinueWithoutRedis() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("hotels");
        CacheErrorHandler errorHandler = new RedisCacheConfig().errorHandler();
        RuntimeException exception = new IllegalStateException("Redis is unavailable");

        assertThatCode(() -> errorHandler.handleCacheGetError(exception, cache, "hotel-id"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCachePutError(exception, cache, "hotel-id", "hotel"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheEvictError(exception, cache, "hotel-id"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheClearError(exception, cache))
                .doesNotThrowAnyException();
    }
}
