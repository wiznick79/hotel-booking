package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HotelRequest(@NotBlank String name, String description, @NotBlank String address,
                           @NotBlank String city, @NotBlank String country,
                           String defaultLanguage,
                           @Size(max = 255)
                           @Pattern(regexp = "^[^\\r\\n<>]*$")
                           String notificationDisplayName,
                           @Email String notificationFromAddress,
                           @Email String notificationReplyToAddress) {

    public HotelRequest(String name, String description, String address, String city, String country,
                        String defaultLanguage) {
        this(name, description, address, city, country, defaultLanguage, null, null, null);
    }
}
