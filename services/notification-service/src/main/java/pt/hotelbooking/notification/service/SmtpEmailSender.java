package pt.hotelbooking.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import pt.hotelbooking.notification.model.Notification;

@Service
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Override
    public void send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(formatFromAddress(notification));
        if (notification.getSenderReplyToAddress() != null
                && !notification.getSenderReplyToAddress().isBlank()) {
            message.setReplyTo(notification.getSenderReplyToAddress());
        }
        message.setTo(notification.getRecipient());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());

        mailSender.send(message);
    }

    private String formatFromAddress(Notification notification) {
        String senderAddress = notification.getSenderFromAddress() == null
                || notification.getSenderFromAddress().isBlank()
                ? fromAddress
                : notification.getSenderFromAddress();
        String displayName = notification.getSenderDisplayName();

        if (displayName == null || displayName.isBlank()) {
            return senderAddress;
        }

        return displayName + " <" + senderAddress + ">";
    }
}
