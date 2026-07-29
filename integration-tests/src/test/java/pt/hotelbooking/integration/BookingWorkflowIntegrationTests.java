package pt.hotelbooking.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import pt.hotelbooking.HotelServiceApplication;
import pt.hotelbooking.booking.BookingServiceApplication;
import pt.hotelbooking.booking.config.NotificationServiceProperties;
import pt.hotelbooking.booking.event.LoggingEventPublisher;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.repository.OutboxEventRepository;
import pt.hotelbooking.booking.service.ReservationService;
import pt.hotelbooking.hotel.model.dto.HotelRequest;
import pt.hotelbooking.hotel.model.dto.RatePeriodRequest;
import pt.hotelbooking.hotel.model.dto.RoomRequest;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.dto.HotelResponse;
import pt.hotelbooking.hotel.model.dto.RoomResponse;
import pt.hotelbooking.hotel.model.dto.RoomTypeResponse;
import pt.hotelbooking.hotel.service.HotelService;
import pt.hotelbooking.hotel.service.RatePeriodService;
import pt.hotelbooking.hotel.service.RoomService;
import pt.hotelbooking.hotel.service.RoomTypeService;
import pt.hotelbooking.notification.NotificationServiceApplication;
import pt.hotelbooking.notification.config.InternalEventsProperties;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BookingWorkflowIntegrationTests {

    private static final String INTERNAL_SERVICE_TOKEN = "change-this-development-token";

    @Test
    void shouldCreateBookingUsingHotelServiceAndDeliverNotificationThroughOutbox() {
        ConfigurableApplicationContext hotelContext = null;
        ConfigurableApplicationContext notificationContext = null;
        ConfigurableApplicationContext bookingContext = null;

        try {
            hotelContext = startHotelService();
            notificationContext = startNotificationService();
            bookingContext = startBookingService(portOf(hotelContext), portOf(notificationContext));

            assertThat(bookingContext.getBean(NotificationServiceProperties.class).url())
                    .isEqualTo("http://localhost:" + portOf(notificationContext));
            assertThat(bookingContext.getBean(NotificationServiceProperties.class).serviceToken())
                    .isEqualTo(INTERNAL_SERVICE_TOKEN);
            assertThat(notificationContext.getBean(InternalEventsProperties.class).serviceToken())
                    .isEqualTo(INTERNAL_SERVICE_TOKEN);

            RoomResponse room = seedHotel(hotelContext);
            ReservationResponse reservation = bookingContext.getBean(ReservationService.class).create(
                    new ReservationRequest(
                            room.hotelId().toString(),
                            "Guest",
                            "+351900000000",
                            "guest@example.com",
                            2,
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(12),
                            null,
                            List.of(room.id().toString()),
                            PaymentMode.PAY_AT_RECEPTION,
                            null));

            OutboxEventRepository outboxEvents = bookingContext.getBean(OutboxEventRepository.class);
            assertThat(outboxEvents.count()).isEqualTo(1);

            bookingContext.getBean(LoggingEventPublisher.class).dispatchPendingEvents();

            assertThat(reservation.id()).isNotNull();
            assertThat(outboxEvents.findAll()).allSatisfy(event ->
                    assertThat(event.getPublishedAt()).isNotNull());
            assertThat(notificationContext.getBean(NotificationRepository.class).count()).isEqualTo(1);
        } finally {
            close(bookingContext);
            close(notificationContext);
            close(hotelContext);
        }
    }

    private ConfigurableApplicationContext startHotelService() {
        return startApplication(HotelServiceApplication.class, commonProperties("hotel-integration"));
    }

    private ConfigurableApplicationContext startNotificationService() {
        Map<String, Object> properties = commonProperties("notification-integration");
        properties.put("internal-events.service-token", INTERNAL_SERVICE_TOKEN);
        properties.put("notification.retry-delay-ms", "3600000");
        properties.put(
                "spring.autoconfigure.exclude",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.actuate.autoconfigure.security.servlet."
                        + "ManagementWebSecurityAutoConfiguration");

        return startApplication(NotificationServiceApplication.class, properties);
    }

    private ConfigurableApplicationContext startBookingService(int hotelPort, int notificationPort) {
        Map<String, Object> properties = commonProperties("booking-integration");
        properties.put("hotel-service.url", "http://localhost:" + hotelPort);
        properties.put("notification-service.url", "http://localhost:" + notificationPort);
        properties.put("notification-service.service-token", INTERNAL_SERVICE_TOKEN);
        properties.put("booking.outbox.dispatch-delay-ms", "3600000");

        return startApplication(BookingServiceApplication.class, properties);
    }

    private Map<String, Object> commonProperties(String databaseName) {
        return new java.util.HashMap<>(Map.ofEntries(
                Map.entry("server.port", "0"),
                Map.entry("spring.datasource.url", "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1"),
                Map.entry("spring.datasource.driver-class-name", "org.h2.Driver"),
                Map.entry("spring.datasource.username", "sa"),
                Map.entry("spring.datasource.password", ""),
                Map.entry("spring.jpa.hibernate.ddl-auto", "create-drop"),
                Map.entry("spring.flyway.enabled", "false"),
                Map.entry("spring.main.banner-mode", "off"),
                Map.entry("jwt.secret", "integration-secret-that-is-long-enough-for-hmac-sha256"),
                Map.entry("JWT_SECRET", "integration-secret-that-is-long-enough-for-hmac-sha256")));
    }

    private RoomResponse seedHotel(ConfigurableApplicationContext hotelContext) {
        HotelService hotelService = hotelContext.getBean(HotelService.class);
        RoomTypeService roomTypeService = hotelContext.getBean(RoomTypeService.class);
        RoomService roomService = hotelContext.getBean(RoomService.class);
        RatePeriodService ratePeriodService = hotelContext.getBean(RatePeriodService.class);

        HotelResponse hotel = hotelService.create(new HotelRequest(
                "Integration Hotel", null, "Main Street", "Lisbon", "Portugal", "en"));
        RoomTypeResponse roomType = roomTypeService.create(new RoomTypeRequest(
                hotel.id(),
                2,
                BigDecimal.valueOf(100),
                Map.of("en", new RoomTypeRequest.TranslationRequest("Double Room", null))), "en");
        RoomResponse room = roomService.create(new RoomRequest(hotel.id(), roomType.id(), "101", 1));
        LocalDate startDate = LocalDate.now().plusDays(1);
        ratePeriodService.create(new RatePeriodRequest(
                roomType.id(),
                startDate,
                startDate.plusYears(1),
                BigDecimal.valueOf(100),
                null,
                "Integration rate"));

        return room;
    }

    private int portOf(ConfigurableApplicationContext context) {
        return ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    }

    private ConfigurableApplicationContext startApplication(
            Class<?> applicationClass,
            Map<String, Object> properties) {
        return startApplication(applicationClass, properties, null);
    }

    private ConfigurableApplicationContext startApplication(
            Class<?> applicationClass,
            Map<String, Object> properties,
            String profile) {
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);

        SpringApplicationBuilder builder = new SpringApplicationBuilder(applicationClass)
                .properties(properties);

        if (profile != null) {
            builder.profiles(profile);
        }

        return builder.run(arguments);
    }

    private void close(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }
}
