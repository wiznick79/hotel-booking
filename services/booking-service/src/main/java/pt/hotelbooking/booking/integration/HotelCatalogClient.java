package pt.hotelbooking.booking.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HotelCatalogClient {
    private final RestClient restClient;

    @Value("${hotel-service.url:http://localhost:8081}")
    private String hotelServiceUrl;

    public HotelCatalogClient(
            RestClient.Builder restClientBuilder,
            @Value("${hotel-service.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${hotel-service.read-timeout-ms:5000}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public boolean roomExists(String roomId) {
        try {
            restClient.get()
                    .uri("/api/rooms/{id}", roomId).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            return false;
        }
    }

    public RoomDetails getRoom(String roomId) {
        try {
            return restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri("/api/rooms/{id}", roomId)
                    .retrieve()
                    .body(RoomDetails.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel service is unavailable.", exception);
        }
    }

    public boolean hotelIsActive(String hotelId) {
        try {
            HotelDetails hotel = restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri("/api/hotels/{id}", hotelId)
                    .retrieve()
                    .body(HotelDetails.class);

            return hotel != null && hotel.active();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel service is unavailable.", exception);
        }
    }

    public BigDecimal quoteRoomType(
            UUID roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        RateQuoteResponse response;
        try {
            response = restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rate-periods/quote")
                            .queryParam("roomTypeId", roomTypeId)
                            .queryParam("checkInDate", checkInDate)
                            .queryParam("checkOutDate", checkOutDate)
                            .build())
                    .retrieve()
                    .body(RateQuoteResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel service is unavailable.", exception);
        }

        if (response == null || response.totalPrice() == null) {
            throw new IllegalStateException("Hotel service returned an empty rate quote.");
        }

        return response.totalPrice();
    }

    public boolean roomIsAvailable(String roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        try {
            Boolean available = restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/room-unavailabilities/room/{roomId}/availability")
                            .queryParam("fromDate", checkInDate)
                            .queryParam("toDate", checkOutDate)
                            .build(roomId))
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(available);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel service is unavailable.", exception);
        }
    }

    public record RoomDetails(
            UUID id,
            UUID hotelId,
            UUID roomTypeId,
            String roomNumber,
            Integer floor,
            String status,
            boolean active) {
    }

    private record HotelDetails(
            UUID id,
            String name,
            boolean active) {
    }

    private record RateQuoteResponse(
            UUID roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            BigDecimal totalPrice) {
    }
}
