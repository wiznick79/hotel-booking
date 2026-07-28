package pt.hotelbooking.identity.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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

@Configuration
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class IdentityDataInitializer {

    private final IdentityRoleRepository roleRepository;

    private final IdentityUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedIdentityData() {
        return arguments -> {
            IdentityRole adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(this::createAdminRole);

            if (userRepository.findByUsername("admin").isEmpty()) {
                IdentityUser admin = new IdentityUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("change-me"));
                admin.getRoles().add(adminRole);
                userRepository.save(admin);
            }
        };
    }

    private IdentityRole createAdminRole() {
        IdentityRole role = new IdentityRole();
        role.setName("ADMIN");
        role.setPermissions(EnumSet.allOf(Permission.class));

        return roleRepository.save(role);
    }
}
