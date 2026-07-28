package pt.hotelbooking.booking.service;

import pt.hotelbooking.booking.model.entity.Reservation;

public interface NotificationService {

    void sendGuestAccessLink(Reservation reservation, String rawToken);
}
