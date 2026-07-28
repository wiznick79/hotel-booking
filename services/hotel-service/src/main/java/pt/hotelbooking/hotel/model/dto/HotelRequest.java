package pt.hotelbooking.hotel.model.dto;

import jakarta.validation.constraints.NotBlank;

public record HotelRequest(@NotBlank String name, String description, @NotBlank String address,
                           @NotBlank String city, @NotBlank String country,
                           String defaultLanguage) {
}
