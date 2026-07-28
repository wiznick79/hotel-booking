package pt.hotelbooking.identity.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.hotelbooking.identity.auth.model.Permission;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    @Mock
    private IdentityUserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService service;

    @Test
    void mapsRolesAndPermissionsToAuthorities() {
        IdentityRole role = new IdentityRole();
        role.setName("STAFF");
        role.getPermissions().add(Permission.ROOM_MANAGE);

        IdentityUser user = new IdentityUser();
        user.setUsername("staff");
        user.setPassword("encoded");
        user.getRoles().add(role);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("staff");

        assertThat(details.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_STAFF", "ROOM_MANAGE");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}
