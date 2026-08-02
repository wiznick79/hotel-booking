package pt.hotelbooking.hotel.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "hotels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hotel extends BaseEntity {

    private String name;
    private String description;
    private String address;
    private String city;
    private String country;

    private String defaultLanguage;
    private String notificationDisplayName;
    private String notificationFromAddress;
    private String notificationReplyToAddress;

    public Hotel(String name, String description, String address, String city, String country,
                 String defaultLanguage, String notificationDisplayName,
                 String notificationFromAddress, String notificationReplyToAddress) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.city = city;
        this.country = country;
        this.defaultLanguage = defaultLanguage;
        this.notificationDisplayName = notificationDisplayName;
        this.notificationFromAddress = notificationFromAddress;
        this.notificationReplyToAddress = notificationReplyToAddress;
    }

    public Hotel(String name, String description, String address, String city, String country,
                 String defaultLanguage) {
        this(name, description, address, city, country, defaultLanguage, null, null, null);
    }

    public void update(String name, String description, String address, String city, String country,
                       String defaultLanguage, String notificationDisplayName,
                       String notificationFromAddress, String notificationReplyToAddress) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.city = city;
        this.country = country;
        this.defaultLanguage = defaultLanguage;
        this.notificationDisplayName = notificationDisplayName;
        this.notificationFromAddress = notificationFromAddress;
        this.notificationReplyToAddress = notificationReplyToAddress;
    }
}
