package pt.hotelbooking.identity.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;

import java.util.Optional;

public interface IdentityUserRepository extends JpaRepository<IdentityUser, Long> {

    Optional<IdentityUser> findByUsername(String username);
}
