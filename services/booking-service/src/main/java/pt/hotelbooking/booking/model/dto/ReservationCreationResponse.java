package pt.hotelbooking.booking.model.dto;

public record ReservationCreationResponse(
        ReservationResponse reservation,
        String guestAccessToken) {
}
