package pt.hotelbooking.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(clientIpAddress(exchange));
    }

    private String clientIpAddress(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null
                || exchange.getRequest().getRemoteAddress().getAddress() == null) {
            return "unknown";
        }

        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
