package pt.hotelbooking.notification.model.dto;

import org.springframework.data.domain.Page;
import pt.hotelbooking.notification.model.Notification;

import java.util.List;

public record ContactMessagePageResponse(
        List<AdminContactMessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static ContactMessagePageResponse from(Page<Notification> messages) {
        return new ContactMessagePageResponse(
                messages.getContent().stream()
                        .map(AdminContactMessageResponse::from)
                        .toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages());
    }
}
