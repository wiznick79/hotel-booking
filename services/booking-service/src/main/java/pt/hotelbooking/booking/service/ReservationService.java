package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import pt.hotelbooking.booking.model.entity.AuditLog;
import pt.hotelbooking.booking.repository.ReservationRepository;
import pt.hotelbooking.booking.repository.AuditLogRepository;
import pt.hotelbooking.booking.repository.BookingPolicyRepository;
import pt.hotelbooking.booking.integration.HotelCatalogClient;
import pt.hotelbooking.booking.model.entity.BookingPolicy;
import pt.hotelbooking.booking.exception.ReservationNotFoundException;
import pt.hotelbooking.booking.exception.RoomReassignmentException;
import pt.hotelbooking.booking.model.dto.RoomReassignmentRequest;
import pt.hotelbooking.booking.model.dto.ReservationModificationRequest;
import pt.hotelbooking.booking.event.EventPublisher;
import pt.hotelbooking.booking.event.ReservationCreatedEvent;

import java.time.LocalDate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final BookingPolicyRepository policyRepo;
    private final HotelCatalogClient hotelCatalogClient;

    private final GuestAccessService guestAccessService;

    private final EventPublisher eventPublisher;

    private final DiscountCodeService discountCodeService;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public ReservationResponse findById(java.util.UUID id) {
        return ReservationResponse.from(reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public ReservationResponse findByGuestAccessToken(String rawToken) {
        Reservation reservation = reservationRepo.findByGuestAccessTokenHash(
                        guestAccessService.hash(rawToken))
                .orElseThrow(() -> new ReservationNotFoundException("Guest reservation not found."));

        if (!reservation.hasValidGuestAccess(Instant.now())) {
            throw new ReservationNotFoundException("Guest reservation not found.");
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByHotelAndDateRange(String hotelId,
                                                             LocalDate from,
                                                             LocalDate to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("The end date must be after the start date.");
        }

        return reservationRepo.findByHotelIdAndCheckInDateLessThanAndCheckOutDateGreaterThan(hotelId, to, from)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findAffectedByRoomAndDateRange(String roomId,
                                                                     LocalDate fromDate,
                                                                     LocalDate toDate) {
        if (!toDate.isAfter(fromDate)) {
            throw new IllegalArgumentException("The end date must be after the start date.");
        }

        return reservationRepo.findAffectedByRoomAndDateRange(
                        roomId,
                        fromDate,
                        toDate,
                        List.of(ReservationStatus.PENDING, ReservationStatus.HELD,
                                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN))
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findMyReservations(String customerUsername) {
        return reservationRepo.findByCustomerUsernameOrderByCheckInDateDesc(customerUsername)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public void cancel(java.util.UUID id) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        cancel(reservation);
    }

    @Transactional
    public void cancelByCustomer(UUID id, String customerUsername) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!customerUsername.equals(reservation.getCustomerUsername())) {
            throw new IllegalStateException("You can only cancel your own reservations.");
        }

        cancel(reservation);
    }

    private void cancel(Reservation reservation) {

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Reservation cannot be cancelled in its current status.");
        }

        BookingPolicy policy = policyRepo.findByHotelId(reservation.getHotelId()).orElse(null);
        if (policy != null && LocalDate.now().isAfter(
                reservation.getCheckInDate().minusDays(policy.getCancellationDeadlineDays()))) {
            throw new IllegalStateException("The cancellation deadline has passed.");
        }

        reservation.cancel();
        reservation.revokeGuestAccess();
        publishReservationEvent("ReservationCancelled", reservation);
    }

    @Transactional
    public ReservationResponse confirm(UUID id, String actor) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        reservation.confirm();
        auditLogRepository.save(new AuditLog(
                actor,
                "RESERVATION_CONFIRMED",
                "Reservation",
                reservation.getId(),
                "Reservation confirmed by staff."));
        publishReservationEvent("ReservationConfirmed", reservation);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse reassignRoom(java.util.UUID id, RoomReassignmentRequest request) {
        return reassignRoom(id, request, "system");
    }

    @Transactional
    public ReservationResponse reassignRoom(java.util.UUID id,
                                             RoomReassignmentRequest request,
                                             String actor) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

        if (reservationRepo.hasBlockingReservation(request.replacementRoomId(), reservation.getCheckOutDate(),
                reservation.getCheckInDate(), blockingStatuses, Instant.now())) {
            throw new RoomReassignmentException("Replacement room is not available.");
        }

        reservation.reassignRoom(request.currentRoomId(), request.replacementRoomId());
        auditLogRepository.save(new AuditLog(
                actor,
                "RESERVATION_ROOM_REASSIGNED",
                "Reservation",
                reservation.getId(),
                request.currentRoomId() + " -> " + request.replacementRoomId()));

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        return create(request, null);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, String customerUsername) {
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        if (!hotelCatalogClient.hotelIsActive(request.hotelId())) {
            throw new IllegalStateException("The hotel is not accepting new bookings.");
        }

        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.HELD,
                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (String roomId : request.roomIds()) {
            HotelCatalogClient.RoomDetails room = hotelCatalogClient.getRoom(roomId);
            if (!room.active() || !"AVAILABLE".equals(room.status())) {
                throw new IllegalStateException("Room is not available for booking: " + roomId);
            }

            if (!hotelCatalogClient.roomIsAvailable(roomId, request.checkInDate(), request.checkOutDate())) {
                throw new IllegalStateException("Room is unavailable during the requested dates: " + roomId);
            }

            if (!room.hotelId().toString().equals(request.hotelId())) {
                throw new IllegalArgumentException("Room does not belong to the requested hotel: " + roomId);
            }

            if (reservationRepo.hasBlockingReservation(roomId, request.checkOutDate(), request.checkInDate(),
                    blockingStatuses, Instant.now())) {
                throw new IllegalStateException("Room is not available: " + roomId);
            }

            totalPrice = totalPrice.add(hotelCatalogClient.quoteRoomType(
                    room.roomTypeId(),
                    request.checkInDate(),
                    request.checkOutDate()));
        }

        BookingPolicy policy = policyRepo.findByHotelId(request.hotelId()).orElse(null);
        if (request.paymentMode() == pt.hotelbooking.booking.model.entity.PaymentMode.PAY_AT_RECEPTION
                && policy != null && !policy.isPayLaterAllowed()) {
            throw new IllegalStateException("This hotel requires payment before booking confirmation.");
        }
        if (request.paymentMode() == pt.hotelbooking.booking.model.entity.PaymentMode.PAY_AT_RECEPTION
                && policy != null && policy.getMaxUnconfirmedBookings() > 0
                && reservationRepo.countByHotelIdAndStatusIn(request.hotelId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.HELD)) >= policy.getMaxUnconfirmedBookings()) {
            throw new IllegalStateException("The hotel has reached its limit for unpaid bookings.");
        }

        Reservation reservation = new Reservation(request.hotelId(), request.guestName(), request.guestPhone(),
                request.guestEmail(), request.guestCount(), request.checkInDate(), request.checkOutDate(), request.notes());
        if (customerUsername != null) {
            reservation.assignCustomer(customerUsername);
        }
        request.roomIds().forEach(reservation::addRoom);

        GuestAccessService.GuestAccessToken guestAccessToken = guestAccessService.createToken(
                request.checkOutDate());
        reservation.configureGuestAccess(guestAccessToken.hash(), guestAccessToken.expiresAt());

        pt.hotelbooking.booking.model.entity.PaymentMode paymentMode = request.paymentMode() == null
                ? pt.hotelbooking.booking.model.entity.PaymentMode.PAY_AT_RECEPTION
                : request.paymentMode();
        reservation.configurePayment(paymentMode, paymentMode != pt.hotelbooking.booking.model.entity.PaymentMode.PAY_NOW);
        reservation.applyPriceSnapshot(totalPrice, "EUR");

        DiscountCodeService.DiscountResult discount = discountCodeService.apply(
                request.hotelId(),
                request.discountCode(),
                totalPrice,
                request.checkInDate());
        reservation.applyPriceSnapshot(discount.total(), "EUR");
        reservation.applyDiscountSnapshot(discount.code(), discount.amount());

        Reservation savedReservation = reservationRepo.save(reservation);
        eventPublisher.publish(new ReservationCreatedEvent(
                savedReservation.getId(),
                savedReservation.getGuestEmail(),
                savedReservation.getGuestName(),
                guestAccessToken.rawToken(),
                savedReservation.getHotelId(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate(),
                savedReservation.getTotalPrice(),
                savedReservation.getCurrency()));

        return ReservationResponse.from(savedReservation);
    }

    @Transactional
    public ReservationResponse modify(UUID id, ReservationModificationRequest request,
                                      String customerUsername) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!customerUsername.equals(reservation.getCustomerUsername())) {
            throw new IllegalStateException("You can only modify your own reservations.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Reservation cannot be modified in its current status.");
        }

        BookingPolicy policy = policyRepo.findByHotelId(reservation.getHotelId()).orElse(null);
        if (policy != null && LocalDate.now().isAfter(
                reservation.getCheckInDate().minusDays(policy.getCancellationDeadlineDays()))) {
            throw new IllegalStateException("The modification deadline has passed.");
        }

        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.PENDING,
                ReservationStatus.HELD, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

        for (String roomId : request.roomIds()) {
            HotelCatalogClient.RoomDetails room = hotelCatalogClient.getRoom(roomId);
            if (!room.active() || !"AVAILABLE".equals(room.status())
                    || !hotelCatalogClient.roomIsAvailable(roomId, request.checkInDate(), request.checkOutDate())
                    || reservationRepo.hasBlockingReservationExcluding(reservation.getId(), roomId,
                    request.checkOutDate(),
                    request.checkInDate(), blockingStatuses, Instant.now())) {
                throw new IllegalStateException("Room is not available for the modified reservation: " + roomId);
            }

            totalPrice = totalPrice.add(hotelCatalogClient.quoteRoomType(room.roomTypeId(),
                    request.checkInDate(), request.checkOutDate()));
        }

        DiscountCodeService.DiscountResult discount = discountCodeService.apply(
                reservation.getHotelId(), request.discountCode(), totalPrice, request.checkInDate());

        reservation.updateDetails(request.guestName(), request.guestPhone(), request.guestEmail(),
                request.guestCount(), request.checkInDate(), request.checkOutDate(), request.notes());
        reservation.replaceRooms(request.roomIds());
        reservation.applyPriceSnapshot(discount.total(), "EUR");
        reservation.applyDiscountSnapshot(discount.code(), discount.amount());

        GuestAccessService.GuestAccessToken accessToken = guestAccessService.createToken(request.checkOutDate());
        reservation.configureGuestAccess(accessToken.hash(), accessToken.expiresAt());

        auditLogRepository.save(new AuditLog(customerUsername, "RESERVATION_MODIFIED",
                "Reservation", reservation.getId(), "Customer modified reservation details."));
        publishReservationEvent("ReservationModified", reservation);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse placeHold(java.util.UUID id, Instant holdUntil) {
        if (!holdUntil.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Hold expiration must be in the future.");
        }

        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
        reservation.placeHold(holdUntil);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public int expireHolds(Instant now) {
        List<Reservation> expiredReservations = reservationRepo
                .findByStatusAndHoldUntilBefore(ReservationStatus.HELD, now);

        for (Reservation reservation : expiredReservations) {
            reservation.expireHold();
            auditLogRepository.save(new AuditLog(
                    "system",
                    "RESERVATION_HOLD_EXPIRED",
                    "Reservation",
                    reservation.getId(),
                    "Temporary hold expired automatically."));
            publishReservationEvent("ReservationHoldExpired", reservation);
        }

        return expiredReservations.size();
    }

    private void publishReservationEvent(String eventType, Reservation reservation) {
        eventPublisher.publish(new pt.hotelbooking.booking.event.ReservationNotificationEvent(
                eventType,
                reservation.getId(),
                reservation.getGuestEmail(),
                reservation.getGuestName(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getTotalPrice(),
                reservation.getCurrency()));
    }
}
