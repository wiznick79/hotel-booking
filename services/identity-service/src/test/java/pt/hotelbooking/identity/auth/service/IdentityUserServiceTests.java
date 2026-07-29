package pt.hotelbooking.identity.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.hotelbooking.identity.auth.model.dto.ChangeOwnPasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.CreateUserRequest;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRoleRepository;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityUserServiceTests {

    @Mock
    private IdentityUserRepository userRepository;

    @Mock
    private IdentityRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private IdentityUserService service;

    @Test
    void createsUserWithEncodedPasswordAndResolvedRole() {
        IdentityRole role = new IdentityRole();
        role.setName("STAFF");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.empty());
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("long-password")).thenReturn("encoded");
        when(userRepository.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IdentityUser user = service.create(new CreateUserRequest(
                "staff", "long-password", Set.of("STAFF"), Set.of(java.util.UUID.randomUUID())));

        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRoles()).containsExactly(role);
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(new IdentityUser()));

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "staff", "long-password", Set.of(), Set.of(java.util.UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already in use.");
    }

    @Test
    void rejectsOwnPasswordChangeWhenCurrentPasswordIsWrong() {
        IdentityUser user = new IdentityUser();
        user.setUsername("staff");
        user.setPassword("encoded-current");
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-current")).thenReturn(false);

        assertThatThrownBy(() -> service.changeOwnPassword(
                "staff", new ChangeOwnPasswordRequest("wrong", "long-new-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current password is incorrect.");
    }
}
