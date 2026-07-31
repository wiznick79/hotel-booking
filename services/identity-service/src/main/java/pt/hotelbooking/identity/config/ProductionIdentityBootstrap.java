package pt.hotelbooking.identity.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.hotelbooking.identity.auth.model.Permission;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRoleRepository;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Configuration
@Profile("postgres")
public class ProductionIdentityBootstrap {

    @Bean
    @ConditionalOnProperty("identity.bootstrap.username")
    CommandLineRunner bootstrapAdministrator(
            IdentityRoleRepository roleRepository,
            IdentityUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            IdentityBootstrapProperties properties) {
        return arguments -> {
            ensureRole(roleRepository, "STAFF");
            ensureRole(roleRepository, "MANAGER");
            ensureRole(roleRepository, "CUSTOMER");
            IdentityRole adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(createAdminRole()));

            if (userRepository.findByUsername(properties.username()).isEmpty()) {
                IdentityUser administrator = new IdentityUser();
                administrator.setUsername(properties.username());
                administrator.setPassword(passwordEncoder.encode(properties.password()));
                administrator.setRoles(Set.of(adminRole));
                administrator.setHotelIds(Set.of(UUID.fromString(properties.hotelId())));
                userRepository.save(administrator);
            }
        };
    }

    private void ensureRole(IdentityRoleRepository roleRepository, String name) {
        roleRepository.findByName(name).orElseGet(() -> {
            IdentityRole role = new IdentityRole();
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private IdentityRole createAdminRole() {
        IdentityRole role = new IdentityRole();
        role.setName("ADMIN");
        role.setPermissions(EnumSet.allOf(Permission.class));
        return role;
    }
}
