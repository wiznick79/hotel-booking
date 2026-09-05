package pt.hotelbooking.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.hotelbooking.hotel.config.HotelScopeAuthorization;
import pt.hotelbooking.hotel.model.dto.WebsiteMediaResponse;
import pt.hotelbooking.hotel.model.entity.WebsiteMedia;
import pt.hotelbooking.hotel.model.entity.WebsiteMediaUsage;
import pt.hotelbooking.hotel.service.WebsiteMediaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class WebsiteMediaController {
    private final WebsiteMediaService service;

    @GetMapping
    public List<WebsiteMediaResponse> findForHotel(@RequestParam UUID hotelId) {
        return service.findForHotel(hotelId);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID id) {
        WebsiteMedia media = service.findEntity(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.getContentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(service.content(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("(#usage.name() == 'ROOM_TYPE_GALLERY' and hasAuthority('ROOM_TYPE_MANAGE')) or "
            + "(#usage.name() != 'ROOM_TYPE_GALLERY' and hasAuthority('HOTEL_MANAGE'))")
    @ResponseStatus(HttpStatus.CREATED)
    public WebsiteMediaResponse upload(@RequestParam UUID hotelId,
                                       @RequestParam(required = false) UUID roomTypeId,
                                       @RequestParam WebsiteMediaUsage usage,
                                       @RequestParam(defaultValue = "0") int sortOrder,
                                       @RequestParam(required = false) String altText,
                                       @RequestPart("file") MultipartFile file,
                                       Authentication authentication) {
        HotelScopeAuthorization.requireAccess(authentication, hotelId);
        return service.upload(hotelId, roomTypeId, usage, sortOrder, altText, file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HOTEL_MANAGE') or hasAuthority('ROOM_TYPE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        WebsiteMedia media = service.findEntity(id);
        HotelScopeAuthorization.requireAccess(authentication, media.getHotel().getId());
        requirePermission(authentication, media.getUsage());
        service.delete(id);
    }

    private void requirePermission(Authentication authentication, WebsiteMediaUsage usage) {
        String required = usage == WebsiteMediaUsage.ROOM_TYPE_GALLERY
                ? "ROOM_TYPE_MANAGE" : "HOTEL_MANAGE";
        boolean granted = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(required));
        if (!granted) throw new AccessDeniedException("You do not have permission to manage this image.");
    }
}
