package pt.hotelbooking.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Profile("!test")
public class InternalEventSecurityFilter extends OncePerRequestFilter {

    private final String serviceToken;

    public InternalEventSecurityFilter(InternalEventsProperties properties) {
        this.serviceToken = properties.serviceToken();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/events")
                && !serviceToken.equals(request.getHeader("X-Internal-Service-Token"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal service token.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
