package pt.hotelbooking.booking.event;

public interface EventPublisher {

    void publish(ReservationCreatedEvent event);

    void publish(ReservationNotificationEvent event);
}
