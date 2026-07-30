package pt.hotelbooking.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Value("${app.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Bean
    KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(clientIpAddress(exchange));
    }

    private String clientIpAddress(ServerWebExchange exchange) {
        if (trustForwardedHeaders) {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",", 2)[0].trim();
            }
        }

        if (exchange.getRequest().getRemoteAddress() == null
                || exchange.getRequest().getRemoteAddress().getAddress() == null) {
            return "unknown";
        }

        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
