package pt.hotelbooking.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitConfigTests {

    @Test
    void shouldUseForwardedClientIpWhenEnabled() {
        GatewayRateLimitConfig configuration = new GatewayRateLimitConfig();
        ReflectionTestUtils.setField(configuration, "trustForwardedHeaders", true);

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "198.51.100.42")
                .remoteAddress(new InetSocketAddress("172.20.0.5", 8080)));

        String clientIp = configuration.clientIpKeyResolver().resolve(exchange).block();

        assertThat(clientIp).isEqualTo("198.51.100.42");
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedHeadersAreDisabled() {
        GatewayRateLimitConfig configuration = new GatewayRateLimitConfig();
        ReflectionTestUtils.setField(configuration, "trustForwardedHeaders", false);

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "198.51.100.42")
                .remoteAddress(new InetSocketAddress("192.0.2.42", 8080)));

        String clientIp = configuration.clientIpKeyResolver().resolve(exchange).block();

        assertThat(clientIp).isEqualTo("192.0.2.42");
    }
}
