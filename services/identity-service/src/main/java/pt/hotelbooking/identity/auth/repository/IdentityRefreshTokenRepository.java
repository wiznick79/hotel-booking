package pt.hotelbooking.identity.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.identity.auth.model.entity.IdentityRefreshToken;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface IdentityRefreshTokenRepository extends JpaRepository<IdentityRefreshToken, UUID> {

    Optional<IdentityRefreshToken> findByTokenHash(String tokenHash);

    List<IdentityRefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
