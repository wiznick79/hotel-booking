package pt.hotelbooking.identity.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;

import java.util.Optional;

public interface IdentityRoleRepository extends JpaRepository<IdentityRole, Long> {

    Optional<IdentityRole> findByName(String name);
}
