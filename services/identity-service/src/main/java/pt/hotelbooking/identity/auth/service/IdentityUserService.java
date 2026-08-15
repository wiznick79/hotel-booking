package pt.hotelbooking.identity.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.identity.auth.model.dto.CreateUserRequest;
import pt.hotelbooking.identity.auth.model.dto.CustomerRegistrationRequest;
import pt.hotelbooking.identity.auth.model.dto.EmailVerificationRequest;
import pt.hotelbooking.identity.auth.model.dto.ChangeOwnPasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdatePasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateRolesRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateHotelAssignmentsRequest;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRoleRepository;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;
import pt.hotelbooking.identity.event.CustomerRegistrationRequestedEvent;
import pt.hotelbooking.identity.event.EmailVerificationTokenCipher;
import pt.hotelbooking.identity.event.IdentityEventPublisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityUserService {

    private final IdentityUserRepository userRepository;
    private final IdentityRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationTokenService authenticationTokenService;
    private final IdentityEventPublisher identityEventPublisher;
    private final EmailVerificationTokenCipher emailVerificationTokenCipher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IdentityUser create(CreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username is already in use.");
        }

        IdentityUser user = new IdentityUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.roles()));
        user.setHotelIds(new HashSet<>(request.hotelIds()));

        return userRepository.save(user);
    }

    @Transactional
    public void registerCustomer(CustomerRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        IdentityUser user = userRepository.findByUsername(email).orElse(null);

        if (user != null && user.isEnabled()) {
            return;
        }

        if (user == null) {
            user = new IdentityUser();
            user.setUsername(email);
            user.setEmail(email);
            user.setFullName(normalizeFullName(request.fullName()));
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setEnabled(false);
            user.setRoles(Set.of(roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new IllegalStateException("Customer role is not configured."))));
        } else {
            user.setFullName(normalizeFullName(request.fullName()));
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        String rawToken = newVerificationToken();
        user.prepareEmailVerification(hash(rawToken), Instant.now().plus(Duration.ofHours(24)));
        IdentityUser savedUser = userRepository.save(user);
        identityEventPublisher.publishCustomerRegistration(new CustomerRegistrationRequestedEvent(
                java.util.UUID.randomUUID(),
                savedUser.getEmail(),
                emailVerificationTokenCipher.encrypt(rawToken)), savedUser.getId());
    }

    @Transactional
    public AuthenticationTokenService.IssuedTokens verifyCustomerEmail(EmailVerificationRequest request) {
        String tokenHash = hash(request.token());
        IdentityUser user = userRepository.findByEmailVerificationTokenHash(tokenHash)
                .filter(candidate -> candidate.hasValidEmailVerificationToken(tokenHash, Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("The email-verification link is invalid or expired."));

        user.verifyEmail();
        return authenticationTokenService.issueTokens(user);
    }

    @Transactional
    public void updateRoles(Long userId, UpdateRolesRequest request) {
        IdentityUser user = findUser(userId);
        user.setRoles(resolveRoles(request.roles()));
    }

    @Transactional
    public void updateHotelAssignments(Long userId, UpdateHotelAssignmentsRequest request) {
        findUser(userId).setHotelIds(new HashSet<>(request.hotelIds()));
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        IdentityUser user = findUser(userId);
        user.setPassword(passwordEncoder.encode(request.password()));
        authenticationTokenService.revokeAllForUser(userId);
    }

    @Transactional
    public void changeOwnPassword(
            String username,
            ChangeOwnPasswordRequest request) {
        IdentityUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        authenticationTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public IdentityUser updateOwnProfile(
            String username,
            String fullName) {
        IdentityUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setFullName(normalizeFullName(fullName));
        return user;
    }

    @Transactional
    public void updateEnabled(Long userId, boolean enabled) {
        IdentityUser user = findUser(userId);
        user.setEnabled(enabled);
    }

    @Transactional(readOnly = true)
    public IdentityUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Transactional(readOnly = true)
    public List<IdentityUser> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IdentityUser> findByRoleNames(Set<String> roleNames) {
        return userRepository.findByRoleNames(roleNames);
    }

    @Transactional(readOnly = true)
    public List<IdentityUser> findStaffAssignedTo(Set<java.util.UUID> hotelIds) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("STAFF")))
                .filter(user -> user.getHotelIds().stream().anyMatch(hotelIds::contains))
                .toList();
    }

    private IdentityUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private Set<IdentityRole> resolveRoles(Set<String> roleNames) {
        Set<IdentityRole> roles = new HashSet<>();

        if (roleNames == null) {
            return roles;
        }

        for (String roleName : roleNames) {
            IdentityRole role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Role not found: " + roleName));
            roles.add(role);
        }

        return roles;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeFullName(String fullName) {
        return fullName.trim().replaceAll("\\s+", " ");
    }

    private String newVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available.", exception);
        }
    }
}
