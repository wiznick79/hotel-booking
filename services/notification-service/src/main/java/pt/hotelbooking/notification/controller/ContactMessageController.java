package pt.hotelbooking.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import pt.hotelbooking.notification.model.dto.AdminContactMessageResponse;
import pt.hotelbooking.notification.model.dto.ContactMessageRequest;
import pt.hotelbooking.notification.model.dto.ContactMessageResponse;
import pt.hotelbooking.notification.model.dto.ContactMessagePageResponse;
import pt.hotelbooking.notification.service.ContactMessageManagementService;
import pt.hotelbooking.notification.service.ContactMessageService;

import java.util.UUID;

@RestController
@RequestMapping("/api/contact-messages")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;
    private final ContactMessageManagementService contactMessageManagementService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ContactMessageResponse send(@Valid @RequestBody ContactMessageRequest request) {
        return contactMessageService.send(request);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
    public ContactMessagePageResponse findByHotel(
            @RequestParam String hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return contactMessageManagementService.findByHotel(
                hotelId,
                page,
                size,
                authentication);
    }

    @PatchMapping("/admin/{messageId}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
    public AdminContactMessageResponse markRead(
            @PathVariable UUID messageId,
            Authentication authentication) {
        return contactMessageManagementService.markRead(messageId, authentication);
    }
}
