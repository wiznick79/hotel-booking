package pt.hotelbooking.identity.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.identity.auth.model.dto.CreateUserRequest;
import pt.hotelbooking.identity.auth.model.dto.ChangeOwnPasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdatePasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateRolesRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateHotelAssignmentsRequest;
import pt.hotelbooking.identity.auth.model.entity.IdentityRole;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRoleRepository;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentityUserService {

    private final IdentityUserRepository userRepository;
    private final IdentityRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
    }

    @Transactional
    public void updateEnabled(Long userId, boolean enabled) {
        IdentityUser user = findUser(userId);
        user.setEnabled(enabled);
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
}
