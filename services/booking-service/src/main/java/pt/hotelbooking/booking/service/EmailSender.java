package pt.hotelbooking.booking.service;

public interface EmailSender {

    void sendGuestAccessLink(String recipient, String reservationId, String rawToken);
}
