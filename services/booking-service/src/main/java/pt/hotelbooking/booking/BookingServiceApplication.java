package pt.hotelbooking.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pt.hotelbooking.booking.config.NotificationServiceProperties;
import pt.hotelbooking.booking.config.OutboxProperties;

@SpringBootApplication
@EnableConfigurationProperties({NotificationServiceProperties.class, OutboxProperties.class})
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
