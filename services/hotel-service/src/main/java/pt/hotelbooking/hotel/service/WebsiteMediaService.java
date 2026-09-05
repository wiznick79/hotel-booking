package pt.hotelbooking.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.hotelbooking.hotel.model.dto.WebsiteMediaResponse;
import pt.hotelbooking.hotel.model.entity.*;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;
import pt.hotelbooking.hotel.repository.HotelRepository;
import pt.hotelbooking.hotel.repository.RoomTypeRepository;
import pt.hotelbooking.hotel.repository.WebsiteMediaRepository;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WebsiteMediaService {
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    private final WebsiteMediaRepository repository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final WebsiteMediaStorage storage;

    @Transactional(readOnly = true)
    public List<WebsiteMediaResponse> findForHotel(UUID hotelId) {
        findHotel(hotelId);
        return repository.findByHotelIdAndErasedFalseOrderByUsageAscSortOrderAscCreatedAtAsc(hotelId)
                .stream().map(WebsiteMediaResponse::from).toList();
    }

    @Transactional
    public WebsiteMediaResponse upload(UUID hotelId, UUID roomTypeId, WebsiteMediaUsage usage,
                                       int sortOrder, String altText, MultipartFile file) {
        Hotel hotel = findHotel(hotelId);
        RoomType roomType = roomTypeId == null ? null : roomTypeRepository.findById(roomTypeId)
                .filter(candidate -> !candidate.isErased()).orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        if ((usage == WebsiteMediaUsage.ROOM_TYPE_GALLERY) != (roomType != null))
            throw badRequest("Room type images require a room type; hotel images must not include one.");
        if (roomType != null && !roomType.getHotel().getId().equals(hotelId))
            throw badRequest("The room type does not belong to this hotel.");

        byte[] bytes = validatedBytes(file);
        String contentType = file.getContentType();
        String key = UUID.randomUUID() + EXTENSIONS.get(contentType);
        storage.store(key, bytes);
        try {
            List<WebsiteMedia> replaced = usage == WebsiteMediaUsage.HERO
                    ? repository.findByHotelIdAndUsageAndErasedFalse(hotelId, usage) : List.of();
            WebsiteMedia media = repository.save(new WebsiteMedia(hotel, roomType, usage,
                    Math.max(0, sortOrder), clean(altText, 255), originalFilename(file),
                    key, contentType, bytes.length));
            replaced.forEach(this::remove);
            return WebsiteMediaResponse.from(media);
        } catch (RuntimeException exception) {
            storage.delete(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WebsiteMedia findEntity(UUID id) {
        return repository.findById(id).filter(media -> !media.isErased())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Website image was not found."));
    }

    @Transactional(readOnly = true)
    public Resource content(UUID id) { return storage.load(findEntity(id).getStorageKey()); }

    @Transactional
    public void delete(UUID id) { remove(findEntity(id)); }

    private void remove(WebsiteMedia media) {
        storage.delete(media.getStorageKey());
        repository.delete(media);
    }

    private byte[] validatedBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("Choose an image to upload.");
        if (file.getSize() > MAX_BYTES) throw badRequest("Images must be 8 MB or smaller.");
        if (!EXTENSIONS.containsKey(file.getContentType())) throw badRequest("Only JPEG and PNG images are supported.");
        try {
            byte[] bytes = file.getBytes();
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw badRequest("The uploaded file is not a valid image.");
            if (image.getWidth() > 12_000 || image.getHeight() > 12_000
                    || (long) image.getWidth() * image.getHeight() > 40_000_000L)
                throw badRequest("The image dimensions are too large.");
            return bytes;
        } catch (IOException exception) {
            throw badRequest("The uploaded image could not be read.");
        }
    }

    private String clean(String value, int max) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned.substring(0, Math.min(cleaned.length(), max));
    }

    private String originalFilename(MultipartFile file) {
        String cleaned = clean(file.getOriginalFilename(), 255);
        return cleaned == null ? "image" + EXTENSIONS.get(file.getContentType()) : cleaned;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Hotel findHotel(UUID id) {
        return hotelRepository.findById(id).filter(hotel -> !hotel.isErased())
                .orElseThrow(() -> new HotelNotFoundException(id));
    }
}
