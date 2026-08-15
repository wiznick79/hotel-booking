package pt.hotelbooking.identity.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IdentityUserRepository extends JpaRepository<IdentityUser, Long> {

    Optional<IdentityUser> findByUsername(String username);

    Optional<IdentityUser> findByEmailVerificationTokenHash(String tokenHash);

    @Query("select distinct identityUser from IdentityUser identityUser "
            + "join identityUser.roles role where role.name in :roleNames")
    List<IdentityUser> findByRoleNames(@Param("roleNames") Set<String> roleNames);
}
