package pt.hotelbooking.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamFallbackControllerTests {

    private final DownstreamFallbackController controller = new DownstreamFallbackController();

    @Test
    void returnsSafeServiceUnavailableProblem() {
        var response = controller.unavailable("hotel-service");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Service temporarily unavailable");
        assertThat(response.getBody().getProperties()).containsEntry("service", "hotel-service");
    }
}
