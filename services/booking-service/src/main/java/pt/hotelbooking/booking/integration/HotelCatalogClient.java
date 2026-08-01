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
import java.util.function.Supplier;
import org.slf4j.MDC;

@Component
public class HotelCatalogClient {
    private final RestClient restClient;
    private final String hotelServiceUrl;

    public HotelCatalogClient(
            RestClient.Builder restClientBuilder,
            @Value("${hotel-service.url:http://localhost:8081}") String hotelServiceUrl,
            @Value("${hotel-service.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${hotel-service.read-timeout-ms:5000}") long readTimeoutMs) {
        this.hotelServiceUrl = hotelServiceUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-Id", correlationId);
                    }

                    return execution.execute(request, body);
                })
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
        return executeHotelCall("retrieve room", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                        .uri("/api/rooms/{id}", roomId)
                        .retrieve()
                        .body(RoomDetails.class));
    }

    public RoomTypeDetails getRoomType(String roomTypeId) {
        return executeHotelCall("retrieve room type", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                        .uri("/api/room-types/{id}", roomTypeId)
                        .retrieve()
                        .body(RoomTypeDetails.class));
    }

    public java.util.List<RoomTypeCatalogItem> findRoomTypes() {
        RoomTypeCatalogItem[] roomTypes = executeHotelCall("retrieve room types", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                        .uri("/api/room-types")
                        .retrieve()
                        .body(RoomTypeCatalogItem[].class));

        return roomTypes == null ? java.util.List.of() : java.util.List.of(roomTypes);
    }

    public java.util.List<RoomDetails> findBookableRooms(
            String hotelId,
            String roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        RoomDetails[] rooms = executeHotelCall("retrieve bookable rooms", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rooms/bookable")
                            .queryParam("hotelId", hotelId)
                            .queryParam("roomTypeId", roomTypeId)
                            .queryParam("fromDate", checkInDate)
                            .queryParam("toDate", checkOutDate)
                            .build())
                    .retrieve()
                    .body(RoomDetails[].class));

        return rooms == null ? java.util.List.of() : java.util.List.of(rooms);
    }

    public boolean hotelIsActive(String hotelId) {
        HotelDetails hotel = executeHotelCall("retrieve hotel", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri("/api/hotels/{id}", hotelId)
                    .retrieve()
                    .body(HotelDetails.class));

        return hotel != null && hotel.active();
    }

    public BigDecimal quoteRoomType(
            UUID roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        RateQuoteResponse response = executeHotelCall("quote room type", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/pricing-rules/quote")
                            .queryParam("roomTypeId", roomTypeId)
                            .queryParam("checkInDate", checkInDate)
                            .queryParam("checkOutDate", checkOutDate)
                            .build())
                    .retrieve()
                    .body(RateQuoteResponse.class));

        if (response == null || response.totalPrice() == null) {
            throw new IllegalStateException("Hotel service returned an empty rate quote.");
        }

        return response.totalPrice();
    }

    public boolean roomIsAvailable(String roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        Boolean available = executeHotelCall("check room availability", () ->
                restClient.mutate().baseUrl(hotelServiceUrl).build().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/room-unavailabilities/room/{roomId}/availability")
                            .queryParam("fromDate", checkInDate)
                            .queryParam("toDate", checkOutDate)
                            .build(roomId))
                    .retrieve()
                    .body(Boolean.class));

        return Boolean.TRUE.equals(available);
    }

    private <T> T executeHotelCall(String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Hotel service rejected the request to " + operation
                            + " with HTTP " + exception.getStatusCode() + ".",
                    exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Hotel service is unavailable while attempting to " + operation + ".", exception);
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

    public record RoomTypeDetails(
            UUID id,
            UUID hotelId,
            int maximumOccupancy,
            boolean active) {
    }

    public record RoomTypeCatalogItem(
            UUID id,
            UUID hotelId,
            int maximumOccupancy,
            BigDecimal basePrice,
            String language,
            String name,
            String description,
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
