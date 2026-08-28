package pt.hotelbooking.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.booking.integration.HotelCatalogClient;
import pt.hotelbooking.booking.event.EventPublisher;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.repository.BookingPolicyRepository;
import pt.hotelbooking.booking.repository.ReservationRepository;
import pt.hotelbooking.booking.repository.AuditLogRepository;
import pt.hotelbooking.booking.model.entity.BookingPolicy;
import pt.hotelbooking.booking.model.dto.RoomAssignmentRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookingPolicyRepository policyRepository;

    @Mock
    private HotelCatalogClient hotelCatalogClient;

    @Mock
    private GuestAccessService guestAccessService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private DiscountCodeService discountCodeService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        lenient().when(hotelCatalogClient.getHotel(anyString())).thenReturn(hotel(true));
    }

    @Test
    void rejectsNewBookingForInactiveHotel() {
        ReservationRequest request = new ReservationRequest(
                "hotel-1",
                "Guest",
                "+351000000000",
                null,
                1,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                null,
                List.of("room-1"),
                null,
                null,
                null,
                true);
        when(hotelCatalogClient.getHotel("hotel-1")).thenReturn(hotel(false));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The hotel is not accepting new bookings.");
    }

    @Test
    void returnsAvailableRoomTypesWithDateSpecificPrices() {
        String hotelId = "00000000-0000-0000-0000-000000000001";
        UUID roomTypeId = UUID.randomUUID();
        LocalDate checkInDate = LocalDate.now().plusDays(10);
        LocalDate checkOutDate = checkInDate.plusDays(2);

        when(hotelCatalogClient.hotelIsActive(hotelId)).thenReturn(true);
        when(hotelCatalogClient.findRoomTypes()).thenReturn(List.of(
                new HotelCatalogClient.RoomTypeCatalogItem(
                        roomTypeId,
                        UUID.fromString(hotelId),
                        2,
                        java.math.BigDecimal.valueOf(80),
                        "en",
                        "Double room",
                        null,
                        true)));
        when(hotelCatalogClient.findBookableRooms(anyString(), anyString(), any(), any())).thenReturn(List.of(
                new HotelCatalogClient.RoomDetails(
                        UUID.randomUUID(),
                        UUID.fromString(hotelId),
                        roomTypeId,
                        "101",
                        1,
                        "AVAILABLE",
                        true)));
        when(reservationRepository.hasBlockingReservation(anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        when(reservationRepository.countUnassignedRoomTypeReservations(anyString(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(hotelCatalogClient.quoteRoomType(roomTypeId, checkInDate, checkOutDate))
                .thenReturn(java.math.BigDecimal.valueOf(160));

        var result = reservationService.searchAvailability(hotelId, checkInDate, checkOutDate, 2);

        assertThat(result).singleElement().satisfies(roomType -> {
            assertThat(roomType.roomTypeId()).isEqualTo(roomTypeId);
            assertThat(roomType.name()).isEqualTo("Double room");
            assertThat(roomType.totalPrice()).isEqualByComparingTo("160");
        });
    }

    @Test
    void cancelsReservationBeforeCancellationDeadline() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(policyRepository.findByHotelId("hotel-1")).thenReturn(Optional.empty());

        reservationService.cancel(id);

        org.assertj.core.api.Assertions.assertThat(reservation.getStatus())
                .isEqualTo(pt.hotelbooking.booking.model.entity.ReservationStatus.CANCELLED);
    }

    @Test
    void rejectsCancellationAfterConfiguredDeadline() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null);
        BookingPolicy policy = new BookingPolicy(
                "hotel-1", true, 10, Duration.ofMinutes(60));
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(policyRepository.findByHotelId("hotel-1")).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> reservationService.cancel(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The cancellation deadline has passed.");
    }

    @Test
    void rejectsCancellingHistoricalReservation() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), null);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Historical reservations cannot be changed.");
    }

    @Test
    void rejectsConfirmingHistoricalReservation() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), null);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(id, "staff"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Historical reservations cannot be changed.");
    }

    @Test
    void checksInConfirmedReservationDuringItsStay() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now(), LocalDate.now().plusDays(2), null);
        reservation.confirm();
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        var result = reservationService.checkIn(id, "staff");

        assertThat(result.status())
                .isEqualTo(pt.hotelbooking.booking.model.entity.ReservationStatus.CHECKED_IN);
    }

    @Test
    void checksOutCheckedInReservation() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), null);
        reservation.confirm();
        reservation.checkIn();
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        var result = reservationService.checkOut(id, "staff");

        assertThat(result.status())
                .isEqualTo(pt.hotelbooking.booking.model.entity.ReservationStatus.CHECKED_OUT);
    }

    @Test
    void marksPastConfirmedReservationAsNoShow() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), null);
        reservation.confirm();
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        var result = reservationService.markNoShow(id, "staff");

        assertThat(result.status())
                .isEqualTo(pt.hotelbooking.booking.model.entity.ReservationStatus.NO_SHOW);
    }

    @Test
    void rejectsBookingWhenRoomIsAlreadyReserved() {
        String hotelId = "00000000-0000-0000-0000-000000000001";
        ReservationRequest request = new ReservationRequest(
                hotelId, "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of("room-1"), null, null, null, true);
        UUID roomTypeId = UUID.randomUUID();
        when(hotelCatalogClient.getRoomType("room-1")).thenReturn(new HotelCatalogClient.RoomTypeDetails(
                roomTypeId, UUID.fromString(hotelId), 2, true));
        when(hotelCatalogClient.findBookableRooms(anyString(), anyString(), any(), any())).thenReturn(List.of(
                new HotelCatalogClient.RoomDetails(UUID.randomUUID(), UUID.fromString(hotelId), roomTypeId,
                        "101", 1, "AVAILABLE", true)));
        when(reservationRepository.countUnassignedRoomTypeReservations(anyString(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(hotelCatalogClient.quoteRoomType(any(), any(), any()))
                .thenReturn(java.math.BigDecimal.valueOf(100));
        when(reservationRepository.hasBlockingReservation(
                anyString(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No room of the selected type is available for the requested dates.");
    }

    @Test
    void createsReservationWithPriceSnapshot() {
        String hotelId = "00000000-0000-0000-0000-000000000001";
        ReservationRequest request = new ReservationRequest(
                hotelId, "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of("room-1"), null, null, null, true);
        UUID roomTypeId = UUID.randomUUID();
        when(hotelCatalogClient.getRoomType("room-1")).thenReturn(new HotelCatalogClient.RoomTypeDetails(
                roomTypeId, UUID.fromString(hotelId), 2, true));
        when(hotelCatalogClient.findBookableRooms(anyString(), anyString(), any(), any())).thenReturn(List.of(
                new HotelCatalogClient.RoomDetails(UUID.randomUUID(), UUID.fromString(hotelId), roomTypeId,
                        "101", 1, "AVAILABLE", true)));
        when(reservationRepository.countUnassignedRoomTypeReservations(anyString(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(reservationRepository.hasBlockingReservation(
                anyString(), any(), any(), any(), any())).thenReturn(false);
        when(hotelCatalogClient.quoteRoomType(any(), any(), any()))
                .thenReturn(java.math.BigDecimal.valueOf(100));
        when(guestAccessService.createToken(any()))
                .thenReturn(new GuestAccessService.GuestAccessToken(
                        "raw-token", "hash", java.time.Instant.now().plusSeconds(3600)));
        when(discountCodeService.apply(anyString(), any(), any(), any()))
                .thenReturn(new DiscountCodeService.DiscountResult(
                        null, java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(100)));
        when(reservationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = reservationService.create(request);

        org.assertj.core.api.Assertions.assertThat(response.totalPrice())
                .isEqualByComparingTo("100");
        org.mockito.Mockito.verify(eventPublisher).publish(any(pt.hotelbooking.booking.event.ReservationCreatedEvent.class));
    }

    @Test
    void rejectsHoldWithPastExpiration() {
        assertThatThrownBy(() -> reservationService.placeHold(
                UUID.randomUUID(), java.time.Instant.now().minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hold expiration must be in the future.");
    }

    @Test
    void reassignsRoomWhenReplacementIsAvailable() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null);
        reservation.addRoomType("room-type-1");
        UUID itemId = UUID.randomUUID();
        ReflectionTestUtils.setField(reservation.getItems().getFirst(), "id", itemId);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasBlockingReservationExcluding(
                any(), anyString(), any(), any(), any(), any())).thenReturn(false);
        UUID hotelId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(hotelCatalogClient.getRoom("room-2")).thenReturn(new HotelCatalogClient.RoomDetails(
                UUID.randomUUID(), hotelId, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "102", 1, "AVAILABLE", true));
        when(hotelCatalogClient.roomIsAvailable(anyString(), any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(reservation, "hotelId", hotelId.toString());
        ReflectionTestUtils.setField(reservation.getItems().getFirst(), "roomTypeId",
                "00000000-0000-0000-0000-000000000001");

        var result = reservationService.assignRoom(
                id,
                itemId,
                new RoomAssignmentRequest("room-2"));

        assertThat(result.items()).extracting(item -> item.roomId()).containsExactly("room-2");
    }

    @Test
    void rejectsReassignmentWhenReplacementIsOccupied() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null);
        reservation.addRoomType("room-type-1");
        UUID itemId = UUID.randomUUID();
        ReflectionTestUtils.setField(reservation.getItems().getFirst(), "id", itemId);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        UUID hotelId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(hotelCatalogClient.getRoom("room-2")).thenReturn(new HotelCatalogClient.RoomDetails(
                UUID.randomUUID(), hotelId, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "102", 1, "AVAILABLE", true));
        when(hotelCatalogClient.roomIsAvailable(anyString(), any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(reservation.getItems().getFirst(), "roomTypeId",
                "00000000-0000-0000-0000-000000000001");
        ReflectionTestUtils.setField(reservation, "hotelId", hotelId.toString());
        when(reservationRepository.hasBlockingReservationExcluding(
                any(), anyString(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.assignRoom(
                id,
                itemId,
                new RoomAssignmentRequest("room-2")))
                .isInstanceOf(pt.hotelbooking.booking.exception.RoomReassignmentException.class)
                .hasMessage("Room is not available.");
    }

    private HotelCatalogClient.HotelDetails hotel(boolean active) {
        return new HotelCatalogClient.HotelDetails(
                UUID.randomUUID(),
                "Hotel Morgadinha",
                "Hotel Morgadinha",
                "morgadinha@wiznick.net",
                "morgadinha@wiznick.net",
                active);
    }

    private ReservationRequest requestWithRoom(String roomId) {
        return new ReservationRequest(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of(roomId), null, null, null, true);
    }
}
