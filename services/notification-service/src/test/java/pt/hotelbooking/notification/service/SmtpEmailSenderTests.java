package pt.hotelbooking.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import pt.hotelbooking.notification.model.Notification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTests {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new SmtpEmailSender(mailSender, new SmtpResilience(
                CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults()));
        ReflectionTestUtils.setField(emailSender, "fromAddress", "bookings@wiznick.net");
    }

    @Test
    void shouldCreateMessageFromNotification() {
        Notification notification = new Notification(
                UUID.randomUUID(),
                "hotel-1",
                "guest@example.com",
                "Booking confirmed",
                "Your booking is confirmed.");

        emailSender.send(notification);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("bookings@wiznick.net");
        assertThat(message.getTo()).containsExactly("guest@example.com");
        assertThat(message.getSubject()).isEqualTo("Booking confirmed");
        assertThat(message.getText()).isEqualTo("Your booking is confirmed.");
    }

    @Test
    void shouldUseHotelSpecificSenderAndReplyToAddress() {
        Notification notification = new Notification(
                UUID.randomUUID(),
                "hotel-1",
                "guest@example.com",
                "Booking confirmed",
                "Your booking is confirmed.",
                "Hotel Morgadinha",
                "morgadinha@wiznick.net",
                "reservas@hotelmorgadinha.pt");

        emailSender.send(notification);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("Hotel Morgadinha <morgadinha@wiznick.net>");
        assertThat(message.getReplyTo()).isEqualTo("reservas@hotelmorgadinha.pt");
    }
}
