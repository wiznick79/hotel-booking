package pt.hotelbooking.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.notification.event.ReservationCreatedEvent;
import pt.hotelbooking.notification.event.ReservationNotificationEvent;
import pt.hotelbooking.notification.service.NotificationProcessor;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class NotificationEventController {

    private final NotificationProcessor notificationProcessor;

    @PostMapping("/reservation-created")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reservationCreated(@RequestBody ReservationCreatedEvent event) {
        notificationProcessor.processReservationCreated(event);
    }

    @PostMapping("/reservation-event")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reservationEvent(@RequestBody ReservationNotificationEvent event) {
        notificationProcessor.processReservationEvent(event);
    }
}
