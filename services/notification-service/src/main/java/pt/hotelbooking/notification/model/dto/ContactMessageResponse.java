package pt.hotelbooking.notification.model.dto;

import java.util.UUID;

public record ContactMessageResponse(UUID referenceId, String status) {
}
