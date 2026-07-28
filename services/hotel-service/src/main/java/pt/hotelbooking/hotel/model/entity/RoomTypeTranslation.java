package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "room_type_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "language_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomTypeTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    private String languageCode;

    private String name;

    private String description;

    public RoomTypeTranslation(String languageCode, String name, String description) {
        this.languageCode = languageCode;
        this.name = name;
        this.description = description;
    }

    void assignRoomType(RoomType roomType) {
        this.roomType = roomType;
    }
}
