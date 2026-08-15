package pt.hotelbooking.notification.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class HotelContactClient {

    private final RestClient restClient;

    public HotelContactClient(
            RestClient.Builder restClientBuilder,
            @Value("${hotel-service.url:http://localhost:8081}") String hotelServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(hotelServiceUrl)
                .build();
    }

    public HotelContactDetails getHotel(UUID hotelId) {
        try {
            HotelContactDetails hotel = restClient.get()
                    .uri("/api/hotels/{id}", hotelId)
                    .retrieve()
                    .body(HotelContactDetails.class);

            if (hotel == null || !hotel.active()) {
                throw new IllegalStateException("The selected hotel is not available.");
            }

            return hotel;
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel contact information is temporarily unavailable.", exception);
        }
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
