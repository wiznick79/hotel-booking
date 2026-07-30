package pt.hotelbooking.notification.service;

import pt.hotelbooking.notification.model.Notification;

public interface EmailSender {

    void send(Notification notification);
}
