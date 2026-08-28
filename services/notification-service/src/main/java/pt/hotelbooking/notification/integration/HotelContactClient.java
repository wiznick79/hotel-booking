package pt.hotelbooking.notification.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class HotelContactClient {

    private final RestClient restClient;
    private final HotelServiceResilience resilience;

    public HotelContactClient(
            RestClient.Builder restClientBuilder,
            HotelServiceResilience resilience,
            @Value("${hotel-service.url:http://localhost:8081}") String hotelServiceUrl,
            @Value("${hotel-service.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${hotel-service.read-timeout-ms:5000}") long readTimeoutMs) {
        this.resilience = resilience;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = restClientBuilder
                .baseUrl(hotelServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public HotelContactDetails getHotel(UUID hotelId) {
        return resilience.execute("retrieve hotel contact information", () -> {
            try {
                HotelContactDetails hotel = restClient.get()
                        .uri("/api/hotels/{id}", hotelId)
                        .retrieve()
                        .body(HotelContactDetails.class);

                if (hotel == null || !hotel.active()) {
                    throw new IllegalStateException("The selected hotel is not available.");
                }

                return hotel;
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is5xxServerError()) {
                    throw new HotelServiceUnavailableException(
                            "Hotel contact information is temporarily unavailable.",
                            exception);
                }

                throw new IllegalStateException("The selected hotel is not available.", exception);
            } catch (RestClientException exception) {
                throw new HotelServiceUnavailableException(
                        "Hotel contact information is temporarily unavailable.",
                        exception);
            }
        });
    }

    public record HotelContactDetails(
            UUID id,
            String name,
            String notificationDisplayName,
            String notificationFromAddress,
            String notificationReplyToAddress,
            boolean active) {
    }
}
