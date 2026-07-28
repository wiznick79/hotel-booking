package pt.hotelbooking.booking.service;

import org.junit.jupiter.api.Test;
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
                null);
        when(hotelCatalogClient.hotelIsActive("hotel-1")).thenReturn(false);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The hotel is not accepting new bookings.");
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
    void rejectsBookingWhenRoomIsAlreadyReserved() {
        String hotelId = "00000000-0000-0000-0000-000000000001";
        ReservationRequest request = new ReservationRequest(
                hotelId, "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of("room-1"), null, null);
        when(hotelCatalogClient.hotelIsActive(hotelId)).thenReturn(true);
        when(hotelCatalogClient.roomIsAvailable(anyString(), any(), any())).thenReturn(true);
        when(hotelCatalogClient.getRoom("room-1")).thenReturn(new HotelCatalogClient.RoomDetails(
                null, UUID.fromString(hotelId), UUID.randomUUID(),
                "101", 1, "AVAILABLE", true));
        when(reservationRepository.hasBlockingReservation(
                anyString(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Room is not available: room-1");
    }

    @Test
    void createsReservationWithPriceSnapshot() {
        String hotelId = "00000000-0000-0000-0000-000000000001";
        ReservationRequest request = new ReservationRequest(
                hotelId, "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of("room-1"), null, null);
        UUID roomTypeId = UUID.randomUUID();
        when(hotelCatalogClient.hotelIsActive(hotelId)).thenReturn(true);
        when(hotelCatalogClient.roomIsAvailable(anyString(), any(), any())).thenReturn(true);
        when(hotelCatalogClient.getRoom("room-1")).thenReturn(new HotelCatalogClient.RoomDetails(
                null, UUID.fromString(hotelId), roomTypeId,
                "101", 1, "AVAILABLE", true));
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
        reservation.addRoom("room-1");
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasBlockingReservation(
                anyString(), any(), any(), any(), any())).thenReturn(false);

        var result = reservationService.reassignRoom(
                id,
                new pt.hotelbooking.booking.model.dto.RoomReassignmentRequest(
                        "room-1", "room-2"));

        assertThat(result.roomIds()).containsExactly("room-2");
    }

    @Test
    void rejectsReassignmentWhenReplacementIsOccupied() {
        UUID id = UUID.randomUUID();
        var reservation = new pt.hotelbooking.booking.model.entity.Reservation(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null);
        reservation.addRoom("room-1");
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasBlockingReservation(
                anyString(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.reassignRoom(
                id,
                new pt.hotelbooking.booking.model.dto.RoomReassignmentRequest(
                        "room-1", "room-2")))
                .isInstanceOf(pt.hotelbooking.booking.exception.RoomReassignmentException.class)
                .hasMessage("Replacement room is not available.");
    }

    private ReservationRequest requestWithRoom(String roomId) {
        return new ReservationRequest(
                "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), null,
                List.of(roomId), null, null);
    }
}
