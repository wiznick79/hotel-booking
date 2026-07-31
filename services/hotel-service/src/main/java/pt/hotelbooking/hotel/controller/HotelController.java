package pt.hotelbooking.hotel.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.hotelbooking.hotel.model.dto.HotelRequest;
import pt.hotelbooking.hotel.model.dto.HotelResponse;
import pt.hotelbooking.hotel.service.HotelService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;

    @GetMapping
    public List<HotelResponse> findAll() {
        return hotelService.findAll();
    }

    @GetMapping("/{id}")
    public HotelResponse findById(@PathVariable UUID id) {
        return hotelService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOTEL_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse create(@Valid @RequestBody HotelRequest request) {
        return hotelService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HOTEL_MANAGE')")
    public HotelResponse update(@PathVariable UUID id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HOTEL_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        hotelService.deactivate(id);
    }
}
