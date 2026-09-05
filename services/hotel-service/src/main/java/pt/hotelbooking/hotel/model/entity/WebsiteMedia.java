package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "website_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebsiteMedia extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebsiteMediaUsage usage;
    private int sortOrder;
    private String altText;
    private String originalFilename;
    private String storageKey;
    private String contentType;
    private long sizeBytes;

    public WebsiteMedia(Hotel hotel, RoomType roomType, WebsiteMediaUsage usage, int sortOrder,
                        String altText, String originalFilename, String storageKey,
                        String contentType, long sizeBytes) {
        this.hotel = hotel;
        this.roomType = roomType;
        this.usage = usage;
        this.sortOrder = sortOrder;
        this.altText = altText;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }
}
