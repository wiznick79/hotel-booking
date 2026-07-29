package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.hotel.model.dto.RoomRequest;
import pt.hotelbooking.hotel.model.dto.RoomResponse;
import pt.hotelbooking.hotel.service.RoomService;
import pt.hotelbooking.hotel.config.HotelScopeAuthorization;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest request, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return roomService.create(request);
    }

    @GetMapping("/{id}")
    public RoomResponse findById(@PathVariable UUID id) {
        return roomService.findById(id);
    }

    @GetMapping
    public List<RoomResponse> findAll() {
        return roomService.findAll();
    }
}
