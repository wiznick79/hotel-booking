package pt.hotelbooking.hotel.model.dto;

import pt.hotelbooking.hotel.model.entity.WebsiteMedia;
import pt.hotelbooking.hotel.model.entity.WebsiteMediaUsage;

import java.util.UUID;

public record WebsiteMediaResponse(UUID id, UUID hotelId, UUID roomTypeId,
                                   WebsiteMediaUsage usage, int sortOrder, String altText,
                                   String originalFilename, String contentType, long sizeBytes,
                                   String url) {
    public static WebsiteMediaResponse from(WebsiteMedia media) {
        return new WebsiteMediaResponse(media.getId(), media.getHotel().getId(),
                media.getRoomType() == null ? null : media.getRoomType().getId(),
                media.getUsage(), media.getSortOrder(), media.getAltText(),
                media.getOriginalFilename(), media.getContentType(), media.getSizeBytes(),
                "/api/media/" + media.getId() + "/content");
    }
}
