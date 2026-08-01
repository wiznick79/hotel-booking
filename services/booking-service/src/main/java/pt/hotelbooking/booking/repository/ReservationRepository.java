package pt.hotelbooking.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.hotelbooking.booking.model.entity.Reservation;
import pt.hotelbooking.booking.model.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByHotelIdAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            String hotelId, LocalDate to, LocalDate from);

    @Query("""
            select distinct r from Reservation r join r.items item
            where item.roomId = :roomId
            and r.checkInDate < :toDate
            and r.checkOutDate > :fromDate
            and r.status in :statuses
            order by r.checkInDate
            """)
    List<Reservation> findAffectedByRoomAndDateRange(@Param("roomId") String roomId,
                                                     @Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate,
                                                     @Param("statuses") Collection<ReservationStatus> statuses);
    @Query("""
            select count(r) > 0 from Reservation r join r.items item
            where item.roomId = :roomId
            and r.checkInDate < :checkOutDate
            and r.checkOutDate > :checkInDate
            and r.status in :statuses
            and (r.status <> pt.hotelbooking.booking.model.entity.ReservationStatus.HELD
                 or r.holdUntil > :now)
            """)
    boolean hasBlockingReservation(@Param("roomId") String roomId,
                                    @Param("checkOutDate") LocalDate checkOutDate,
                                    @Param("checkInDate") LocalDate checkInDate,
                                    @Param("statuses") Collection<ReservationStatus> statuses,
                                    @Param("now") java.time.Instant now);

    @Query("""
            select count(r) > 0 from Reservation r join r.items item
            where r.id <> :reservationId
            and item.roomId = :roomId
            and r.checkInDate < :checkOutDate
            and r.checkOutDate > :checkInDate
            and r.status in :statuses
            and (r.status <> pt.hotelbooking.booking.model.entity.ReservationStatus.HELD
                 or r.holdUntil > :now)
            """)
    boolean hasBlockingReservationExcluding(@Param("reservationId") UUID reservationId,
                                            @Param("roomId") String roomId,
                                            @Param("checkOutDate") LocalDate checkOutDate,
                                            @Param("checkInDate") LocalDate checkInDate,
                                            @Param("statuses") Collection<ReservationStatus> statuses,
                                            @Param("now") java.time.Instant now);

    @Query("""
            select count(item) from Reservation r join r.items item
            where item.roomTypeId = :roomTypeId
            and item.roomId is null
            and r.checkInDate < :checkOutDate
            and r.checkOutDate > :checkInDate
            and r.status in :statuses
            and (r.status <> pt.hotelbooking.booking.model.entity.ReservationStatus.HELD
                 or r.holdUntil > :now)
            """)
    long countUnassignedRoomTypeReservations(@Param("roomTypeId") String roomTypeId,
                                             @Param("checkOutDate") LocalDate checkOutDate,
                                             @Param("checkInDate") LocalDate checkInDate,
                                             @Param("statuses") Collection<ReservationStatus> statuses,
                                             @Param("now") java.time.Instant now);

    long countByHotelIdAndStatusIn(String hotelId, Collection<ReservationStatus> statuses);

    List<Reservation> findByCustomerUsernameOrderByCheckInDateDesc(String customerUsername);

    java.util.Optional<Reservation> findByGuestAccessTokenHash(String guestAccessTokenHash);

    List<Reservation> findByStatusAndHoldUntilBefore(ReservationStatus status, java.time.Instant now);
}
