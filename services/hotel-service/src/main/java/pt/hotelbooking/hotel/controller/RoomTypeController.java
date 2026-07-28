package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.hotel.model.dto.RoomTypeRequest;
import pt.hotelbooking.hotel.model.dto.RoomTypeResponse;
import pt.hotelbooking.hotel.service.RoomTypeService;
import pt.hotelbooking.core.i18n.LanguageResolver;

import java.util.List;

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
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return roomTypeService.create(request, languageResolver.resolve(null, acceptLanguage));
    }

    @GetMapping
    public List<RoomTypeResponse> findAll(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return roomTypeService.findAll(languageResolver.resolve(null, acceptLanguage));
    }
}
