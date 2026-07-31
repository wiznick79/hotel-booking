package pt.hotelbooking.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pt.hotelbooking.HotelServiceApplication;
import pt.hotelbooking.booking.BookingServiceApplication;
import pt.hotelbooking.booking.event.LoggingEventPublisher;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.entity.PaymentMode;
import pt.hotelbooking.booking.repository.OutboxEventRepository;
import pt.hotelbooking.booking.service.ReservationService;
import pt.hotelbooking.hotel.model.dto.HotelRequest;
import pt.hotelbooking.hotel.model.dto.RoomRequest;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.dto.HotelResponse;
import pt.hotelbooking.hotel.model.dto.RoomResponse;
import pt.hotelbooking.hotel.model.dto.RoomTypeResponse;
import pt.hotelbooking.hotel.service.HotelService;
import pt.hotelbooking.hotel.service.RoomService;
import pt.hotelbooking.hotel.service.RoomTypeService;
import pt.hotelbooking.notification.NotificationServiceApplication;
import pt.hotelbooking.notification.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BookingWorkflowIntegrationTests {

    private static final String RESERVATION_EVENTS_TOPIC = "reservation-events";

    private static final String RESERVATION_EVENTS_DLT = RESERVATION_EVENTS_TOPIC + ".DLT";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:4.1.2"));

    @BeforeAll
    static void createKafkaTopics() {
        Map<String, Object> properties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(
                            new NewTopic(RESERVATION_EVENTS_TOPIC, 3, (short) 1),
                            new NewTopic(RESERVATION_EVENTS_DLT, 3, (short) 1)))
                    .all()
                    .get();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create the Kafka topics for the integration test.", exception);
        }
    }

    @Test
    void shouldCreateBookingUsingHotelServiceAndDeliverNotificationThroughOutbox() {
        ConfigurableApplicationContext hotelContext = null;
        ConfigurableApplicationContext notificationContext = null;
        ConfigurableApplicationContext bookingContext = null;

        try {
            hotelContext = startHotelService();
            notificationContext = startNotificationService();
            bookingContext = startBookingService(portOf(hotelContext));

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
            NotificationRepository notifications = notificationContext.getBean(NotificationRepository.class);
            await().untilAsserted(() ->
                    assertThat(notifications.count()).isEqualTo(1));
        } finally {
            close(bookingContext);
            close(notificationContext);
            close(hotelContext);
        }
    }

    @Test
    void shouldSendUnprocessableKafkaEventToDeadLetterTopic() throws Exception {
        ConfigurableApplicationContext notificationContext = null;

        try {
            notificationContext = startNotificationService();
            KafkaTemplate<String, String> kafkaTemplate = notificationContext.getBean(KafkaTemplate.class);

            kafkaTemplate.send(MessageBuilder.withPayload("not-valid-json")
                    .setHeader(KafkaHeaders.TOPIC, RESERVATION_EVENTS_TOPIC)
                    .setHeader(KafkaHeaders.KEY, "invalid-event")
                    .setHeader("eventType", "ReservationCreated")
                    .build()).get();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(totalRecordsInTopic(RESERVATION_EVENTS_DLT)).isEqualTo(1));
        } finally {
            close(notificationContext);
        }
    }

    private ConfigurableApplicationContext startHotelService() {
        return startApplication(HotelServiceApplication.class, commonProperties("hotel-integration"));
    }

    private ConfigurableApplicationContext startNotificationService() {
        Map<String, Object> properties = commonProperties("notification-integration");
        properties.put("notification.retry-delay-ms", "3600000");
        properties.put("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
        properties.put("spring.kafka.consumer.group-id", "notification-service-integration-test");
        properties.put("spring.kafka.consumer.auto-offset-reset", "earliest");
        properties.put("booking-events.topic", RESERVATION_EVENTS_TOPIC);
        properties.put("spring.mail.host", "localhost");
        properties.put("spring.mail.port", "1025");
        properties.put("notification.email.from", "no-reply@integration.test");
        properties.put(
                "spring.autoconfigure.exclude",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.actuate.autoconfigure.security.servlet."
                        + "ManagementWebSecurityAutoConfiguration");

        return startApplication(NotificationServiceApplication.class, properties, "test");
    }

    private ConfigurableApplicationContext startBookingService(int hotelPort) {
        Map<String, Object> properties = commonProperties("booking-integration");
        properties.put("hotel-service.url", "http://localhost:" + hotelPort);
        properties.put("booking.outbox.dispatch-delay-ms", "3600000");
        properties.put("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
        properties.put("booking-events.topic", RESERVATION_EVENTS_TOPIC);

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

    private long totalRecordsInTopic(String topic) {
        Map<String, Object> properties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        Map<TopicPartition, OffsetSpec> offsets = Map.of(
                new TopicPartition(topic, 0), OffsetSpec.latest(),
                new TopicPartition(topic, 1), OffsetSpec.latest(),
                new TopicPartition(topic, 2), OffsetSpec.latest());

        try (AdminClient adminClient = AdminClient.create(properties)) {
            return adminClient.listOffsets(offsets)
                    .all()
                    .get()
                    .values()
                    .stream()
                    .mapToLong(result -> result.offset())
                    .sum();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the Kafka topic offset for the integration test.", exception);
        }
    }

    private RoomResponse seedHotel(ConfigurableApplicationContext hotelContext) {
        HotelService hotelService = hotelContext.getBean(HotelService.class);
        RoomTypeService roomTypeService = hotelContext.getBean(RoomTypeService.class);
        RoomService roomService = hotelContext.getBean(RoomService.class);

        HotelResponse hotel = hotelService.create(new HotelRequest(
                "Integration Hotel", null, "Main Street", "Lisbon", "Portugal", "en"));
        RoomTypeResponse roomType = roomTypeService.create(new RoomTypeRequest(
                hotel.id(),
                2,
                BigDecimal.valueOf(100),
                Map.of("en", new RoomTypeRequest.TranslationRequest("Double Room", null))), "en");
        RoomResponse room = roomService.create(new RoomRequest(hotel.id(), roomType.id(), "101", 1));
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
