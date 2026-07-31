package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.dto.RoomTypeResponse;
import pt.hotelbooking.hotel.service.RoomTypeService;
import pt.hotelbooking.core.i18n.LanguageResolver;
import pt.hotelbooking.hotel.config.HotelScopeAuthorization;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final LanguageResolver languageResolver;

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_TYPE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse create(@Valid @RequestBody RoomTypeRequest request,
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
                                   Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return roomTypeService.create(request, languageResolver.resolve(null, acceptLanguage));
    }

    @GetMapping
    public List<RoomTypeResponse> findAll(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return roomTypeService.findAll(languageResolver.resolve(null, acceptLanguage));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_TYPE_MANAGE')")
    public RoomTypeResponse update(@PathVariable UUID id, @Valid @RequestBody RoomTypeRequest request,
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
                                   Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, request.hotelId());
        return roomTypeService.update(id, request, languageResolver.resolve(null, acceptLanguage));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_TYPE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, roomTypeService.findHotelId(id));
        roomTypeService.deactivate(id);
    }
}
