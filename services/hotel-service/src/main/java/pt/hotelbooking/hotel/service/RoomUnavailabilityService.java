package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.hotel.exception.RoomNotFoundException;
import pt.hotelbooking.hotel.model.dto.RoomUnavailabilityRequest;
import pt.hotelbooking.hotel.model.dto.RoomUnavailabilityResponse;
import pt.hotelbooking.hotel.model.entity.Room;
import pt.hotelbooking.hotel.model.entity.RoomUnavailability;
import pt.hotelbooking.hotel.model.entity.AuditLog;
import pt.hotelbooking.hotel.repository.RoomRepository;
import pt.hotelbooking.hotel.repository.RoomUnavailabilityRepository;
import pt.hotelbooking.hotel.repository.AuditLogRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomUnavailabilityService {

    private final RoomRepository roomRepository;
    private final RoomUnavailabilityRepository unavailabilityRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public RoomUnavailabilityResponse create(RoomUnavailabilityRequest request) {
        return create(request, "system");
    }

    @Transactional
    public RoomUnavailabilityResponse create(RoomUnavailabilityRequest request, String actor) {
        validateDates(request.fromDate(), request.toDate());

        if (request.emergency() && (request.reason() == null || request.reason().isBlank())) {
            throw new IllegalArgumentException("An emergency room block requires a reason.");
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException(request.roomId()));

        if (unavailabilityRepository.overlaps(request.roomId(), request.fromDate(), request.toDate())) {
            throw new IllegalArgumentException("The room is already unavailable during part of this period.");
        }

        RoomUnavailability saved = unavailabilityRepository.save(
                new RoomUnavailability(room, request.fromDate(), request.toDate(), request.reason(), request.emergency()));
        auditLogRepository.save(new AuditLog(actor, "ROOM_UNAVAILABILITY_CREATED",
                "RoomUnavailability", saved.getId(), request.reason()));

        return RoomUnavailabilityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomUnavailabilityResponse> findByRoom(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException(roomId);
        }

        return unavailabilityRepository.findByRoomIdOrderByFromDate(roomId)
                .stream()
                .map(RoomUnavailabilityResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID id, String actor) {
        RoomUnavailability unavailability = unavailabilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room unavailability not found: " + id));

        auditLogRepository.save(new AuditLog(
                actor,
                "ROOM_UNAVAILABILITY_DELETED",
                "RoomUnavailability",
                id,
                unavailability.getReason()));
        unavailabilityRepository.delete(unavailability);
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(UUID roomId, LocalDate fromDate, LocalDate toDate) {
        if (!toDate.isAfter(fromDate)) {
            throw new IllegalArgumentException("The end date must be after the start date.");
        }

        return !unavailabilityRepository.overlaps(roomId, fromDate, toDate);
    }

    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (!toDate.isAfter(fromDate)) {
            throw new IllegalArgumentException("The end date must be after the start date.");
        }
    }
}
