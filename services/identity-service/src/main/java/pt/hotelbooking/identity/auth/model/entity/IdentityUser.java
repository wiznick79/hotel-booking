package pt.hotelbooking.identity.auth.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.UUID;
import java.util.Set;

@Entity
@Table(name = "identity_users")
@Getter
@Setter
@NoArgsConstructor
public class IdentityUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String fullName;

    private String password;

    private boolean enabled = true;

    private String emailVerificationTokenHash;

    private java.time.Instant emailVerificationTokenExpiresAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "identity_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<IdentityRole> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "identity_user_hotels", joinColumns = @JoinColumn(name = "user_id"))
    @jakarta.persistence.Column(name = "hotel_id", nullable = false)
    private Set<UUID> hotelIds = new HashSet<>();

    public boolean hasValidEmailVerificationToken(String tokenHash, java.time.Instant now) {
        return !enabled
                && emailVerificationTokenHash != null
                && emailVerificationTokenHash.equals(tokenHash)
                && emailVerificationTokenExpiresAt != null
                && emailVerificationTokenExpiresAt.isAfter(now);
    }

    public void prepareEmailVerification(String tokenHash, java.time.Instant expiresAt) {
        emailVerificationTokenHash = tokenHash;
        emailVerificationTokenExpiresAt = expiresAt;
    }

    public void verifyEmail() {
        enabled = true;
        emailVerificationTokenHash = null;
        emailVerificationTokenExpiresAt = null;
    }
}
