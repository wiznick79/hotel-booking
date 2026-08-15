package pt.hotelbooking.identity.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.hotelbooking.identity.auth.model.dto.ChangeOwnPasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.CreateUserRequest;
import pt.hotelbooking.identity.auth.model.dto.CustomerRegistrationRequest;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRoleRepository;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;
import pt.hotelbooking.identity.event.EmailVerificationTokenCipher;
import pt.hotelbooking.identity.event.IdentityEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdentityUserServiceTests {

    @Mock
    private IdentityUserRepository userRepository;

    @Mock
    private IdentityRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdentityEventPublisher identityEventPublisher;

    @Mock
    private EmailVerificationTokenCipher emailVerificationTokenCipher;

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

    @Test
    void registersDisabledCustomerAndPublishesVerificationRequest() {
        IdentityRole customerRole = new IdentityRole();
        customerRole.setName("CUSTOMER");
        when(userRepository.findByUsername("guest@example.test")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("long-password")).thenReturn("encoded");
        when(userRepository.save(any(IdentityUser.class))).thenAnswer(invocation -> {
            IdentityUser user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(emailVerificationTokenCipher.encrypt(any(String.class))).thenReturn("encrypted-token");

        service.registerCustomer(new CustomerRegistrationRequest(
                "  Guest   Example  ",
                "Guest@Example.Test",
                "long-password"));

        org.mockito.ArgumentCaptor<IdentityUser> userCaptor = org.mockito.ArgumentCaptor.forClass(IdentityUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("guest@example.test");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("guest@example.test");
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Guest Example");
        assertThat(userCaptor.getValue().isEnabled()).isFalse();
        verify(identityEventPublisher).publishCustomerRegistration(any(), org.mockito.ArgumentMatchers.eq(42L));
    }

    @Test
    void updatesAndNormalizesOwnProfileName() {
        IdentityUser user = new IdentityUser();
        user.setUsername("guest@example.test");
        when(userRepository.findByUsername("guest@example.test")).thenReturn(Optional.of(user));

        IdentityUser updatedUser = service.updateOwnProfile(
                "guest@example.test",
                "  Guest   Example  ");

        assertThat(updatedUser.getFullName()).isEqualTo("Guest Example");
    }
}
