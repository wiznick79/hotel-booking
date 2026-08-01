package pt.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.booking.model.dto.ReservationRequest;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.dto.AvailabilitySearchResponse;
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
import pt.hotelbooking.booking.model.dto.RoomAssignmentRequest;
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

    @Transactional(readOnly = true)
    public long countPendingConfirmations(String hotelId) {
        return reservationRepo.countByHotelIdAndStatusIn(
                hotelId,
                List.of(ReservationStatus.PENDING, ReservationStatus.HELD));
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySearchResponse> searchAvailability(
            String hotelId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int guestCount) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        if (guestCount < 1) {
            throw new IllegalArgumentException("At least one guest is required.");
        }

        if (!hotelCatalogClient.hotelIsActive(hotelId)) {
            return List.of();
        }

        List<ReservationStatus> blockingStatuses = blockingStatuses();

        return hotelCatalogClient.findRoomTypes().stream()
                .filter(roomType -> roomType.hotelId().toString().equals(hotelId))
                .filter(HotelCatalogClient.RoomTypeCatalogItem::active)
                .filter(roomType -> roomType.maximumOccupancy() >= guestCount)
                .filter(roomType -> hasAvailableRoomTypeCapacity(
                        hotelId,
                        roomType.id().toString(),
                        checkInDate,
                        checkOutDate,
                        blockingStatuses))
                .map(roomType -> new AvailabilitySearchResponse(
                        roomType.id(),
                        roomType.name(),
                        roomType.maximumOccupancy(),
                        hotelCatalogClient.quoteRoomType(roomType.id(), checkInDate, checkOutDate),
                        "EUR"))
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
    public ReservationResponse assignRoom(UUID id, UUID itemId, RoomAssignmentRequest request) {
        return assignRoom(id, itemId, request, "system");
    }

    @Transactional
    public ReservationResponse assignRoom(UUID id,
                                          UUID itemId,
                                          RoomAssignmentRequest request,
                                          String actor) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        var item = reservation.getItems().stream()
                .filter(reservationItem -> reservationItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RoomReassignmentException("Reservation item does not belong to this reservation."));
        HotelCatalogClient.RoomDetails room = hotelCatalogClient.getRoom(request.roomId());

        if (!room.active() || !"AVAILABLE".equals(room.status())
                || !room.hotelId().toString().equals(reservation.getHotelId())
                || !room.roomTypeId().toString().equals(item.getRoomTypeId())
                || !hotelCatalogClient.roomIsAvailable(request.roomId(), reservation.getCheckInDate(),
                reservation.getCheckOutDate())) {
            throw new RoomReassignmentException("Room is not suitable for this reservation item.");
        }

        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

        if (reservationRepo.hasBlockingReservationExcluding(
                reservation.getId(),
                request.roomId(),
                reservation.getCheckOutDate(),
                reservation.getCheckInDate(),
                blockingStatuses,
                Instant.now())) {
            throw new RoomReassignmentException("Room is not available.");
        }

        String previousRoomId = item.getRoomId();
        reservation.assignRoom(itemId, request.roomId());
        auditLogRepository.save(new AuditLog(
                actor,
                previousRoomId == null ? "RESERVATION_ROOM_ASSIGNED" : "RESERVATION_ROOM_REASSIGNED",
                "Reservation",
                reservation.getId(),
                String.valueOf(previousRoomId) + " -> " + request.roomId()));

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

        BigDecimal totalPrice = validateAndQuoteRoomTypes(request.hotelId(), request.roomTypeIds(),
                request.guestCount(), request.checkInDate(), request.checkOutDate());

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
        request.roomTypeIds().forEach(reservation::addRoomType);
        autoAssignRooms(reservation, blockingStatuses());

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

        BigDecimal totalPrice = validateAndQuoteRoomTypes(reservation.getHotelId(), request.roomTypeIds(),
                request.guestCount(), request.checkInDate(), request.checkOutDate());

        DiscountCodeService.DiscountResult discount = discountCodeService.apply(
                reservation.getHotelId(), request.discountCode(), totalPrice, request.checkInDate());

        reservation.updateDetails(request.guestName(), request.guestPhone(), request.guestEmail(),
                request.guestCount(), request.checkInDate(), request.checkOutDate(), request.notes());
        reservation.replaceRoomTypes(request.roomTypeIds());
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
                reservation.getHotelId(),
                reservation.getGuestEmail(),
                reservation.getGuestName(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getTotalPrice(),
                reservation.getCurrency()));
    }

    private BigDecimal validateAndQuoteRoomTypes(String hotelId, List<String> roomTypeIds,
                                                 int guestCount, LocalDate checkInDate,
                                                 LocalDate checkOutDate) {
        List<ReservationStatus> blockingStatuses = blockingStatuses();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalCapacity = 0;
        java.util.Map<String, Integer> requestedByType = new java.util.HashMap<>();

        for (String roomTypeId : roomTypeIds) {
            HotelCatalogClient.RoomTypeDetails roomType = hotelCatalogClient.getRoomType(roomTypeId);
            if (!roomType.active() || !roomType.hotelId().toString().equals(hotelId)) {
                throw new IllegalArgumentException("Room type does not belong to the requested hotel: " + roomTypeId);
            }

            requestedByType.merge(roomTypeId, 1, Integer::sum);
            totalCapacity += roomType.maximumOccupancy();
            totalPrice = totalPrice.add(hotelCatalogClient.quoteRoomType(
                    roomType.id(), checkInDate, checkOutDate));
        }

        if (guestCount > totalCapacity) {
            throw new IllegalArgumentException("Selected room types cannot accommodate the requested guests.");
        }

        for (var entry : requestedByType.entrySet()) {
            if (!hasAvailableRoomTypeCapacity(hotelId, entry.getKey(), checkInDate, checkOutDate,
                    blockingStatuses, entry.getValue())) {
                throw new IllegalStateException("No room of the selected type is available for the requested dates.");
            }
        }

        return totalPrice;
    }

    private void autoAssignRooms(Reservation reservation, List<ReservationStatus> blockingStatuses) {
        java.util.Set<String> assignedRoomIds = new java.util.HashSet<>();

        for (var item : reservation.getItems()) {
            HotelCatalogClient.RoomDetails room = hotelCatalogClient.findBookableRooms(
                            reservation.getHotelId(),
                            item.getRoomTypeId(),
                            reservation.getCheckInDate(),
                            reservation.getCheckOutDate())
                    .stream()
                    .filter(candidate -> !assignedRoomIds.contains(candidate.id().toString()))
                    .filter(candidate -> !reservationRepo.hasBlockingReservation(candidate.id().toString(),
                            reservation.getCheckOutDate(), reservation.getCheckInDate(),
                            blockingStatuses, Instant.now()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No room of the selected type is available for the requested dates."));

            reservation.assignFirstUnassignedRoomOfType(item.getRoomTypeId(), room.id().toString());
            assignedRoomIds.add(room.id().toString());
        }
    }

    private List<ReservationStatus> blockingStatuses() {
        return List.of(ReservationStatus.PENDING, ReservationStatus.HELD,
                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
    }

    private boolean hasAvailableRoomTypeCapacity(
            String hotelId,
            String roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            List<ReservationStatus> blockingStatuses) {
        return hasAvailableRoomTypeCapacity(
                hotelId,
                roomTypeId,
                checkInDate,
                checkOutDate,
                blockingStatuses,
                1);
    }

    private boolean hasAvailableRoomTypeCapacity(
            String hotelId,
            String roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            List<ReservationStatus> blockingStatuses,
            int requestedRooms) {
        long availablePhysicalRooms = hotelCatalogClient.findBookableRooms(
                        hotelId,
                        roomTypeId,
                        checkInDate,
                        checkOutDate)
                .stream()
                .filter(room -> !reservationRepo.hasBlockingReservation(
                        room.id().toString(),
                        checkOutDate,
                        checkInDate,
                        blockingStatuses,
                        Instant.now()))
                .count();
        long unassignedReservations = reservationRepo.countUnassignedRoomTypeReservations(
                roomTypeId,
                checkOutDate,
                checkInDate,
                blockingStatuses,
                Instant.now());

        return availablePhysicalRooms - unassignedReservations >= requestedRooms;
    }
}
