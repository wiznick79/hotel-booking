package pt.hotelbooking.notification.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContactMessageRequest(
        @NotNull UUID hotelId,
        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "^[^\\r\\n\\u0000<>]+$")
        String name,
        @NotBlank
        @Size(max = 180)
        @Pattern(regexp = "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
                + "(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+"
                + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
        String email,
        @Size(max = 40)
        @Pattern(regexp = "^[+0-9() .-]*$")
        String phone,
        @NotNull ContactSubject subject,
        @NotBlank
        @Size(min = 10, max = 3000)
        @Pattern(regexp = "^[^\\u0000\\u000B\\u000C\\u000E-\\u001F\\u007F]*$")
        String message,
        @AssertTrue boolean privacyNoticeAccepted,
        @Size(max = 0) String website) {
}
