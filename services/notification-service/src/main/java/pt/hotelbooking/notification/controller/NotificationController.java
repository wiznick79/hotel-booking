package pt.hotelbooking.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.notification.model.dto.NotificationResponse;
import pt.hotelbooking.notification.service.NotificationManagementService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
public class NotificationController {

    private final NotificationManagementService notificationManagementService;

    @GetMapping
    public List<NotificationResponse> findByHotel(
            @RequestParam String hotelId,
            Authentication authentication) {
        return notificationManagementService.findByHotel(hotelId, authentication);
    }

    @PostMapping("/{notificationId}/requeue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse requeue(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        return notificationManagementService.requeue(notificationId, authentication);
    }
}
