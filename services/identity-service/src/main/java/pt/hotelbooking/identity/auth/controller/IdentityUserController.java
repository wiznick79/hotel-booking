package pt.hotelbooking.identity.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.hotelbooking.identity.auth.model.dto.CreateUserRequest;
import pt.hotelbooking.identity.auth.model.dto.ChangeOwnPasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdatePasswordRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateRolesRequest;
import pt.hotelbooking.identity.auth.model.dto.UpdateHotelAssignmentsRequest;
import pt.hotelbooking.identity.auth.model.dto.UserResponse;
import pt.hotelbooking.identity.auth.service.IdentityUserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class IdentityUserController {

    private final IdentityUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE') or hasAuthority('STAFF_MANAGE')")
    public List<UserResponse> findAll(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<pt.hotelbooking.identity.auth.model.entity.IdentityUser> users = isAdmin
                ? userService.findAll()
                : userService.findStaffAssignedTo(hotelIds(authentication));
        return users.stream().map(UserResponse::from).toList();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse findCurrentUser(Authentication authentication) {
        return UserResponse.from(userService.findByUsername(authentication.getName()));
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void changeOwnPassword(
            Authentication authentication,
            @Valid @RequestBody ChangeOwnPasswordRequest request) {
        userService.changeOwnPassword(authentication.getName(), request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and #request.roles.size() == 1 and #request.roles.contains('STAFF'))")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !hotelIds(authentication).containsAll(request.hotelIds())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Staff can only be assigned to hotels managed by the current manager.");
        }
        return UserResponse.from(userService.create(request));
    }

    private java.util.Set<java.util.UUID> hotelIds(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return java.util.Set.of();
        }
        return jwt.getClaimAsStringList("hotelIds").stream()
                .map(java.util.UUID::fromString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @PatchMapping("/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void updateRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRolesRequest request) {
        userService.updateRoles(userId, request);
    }

    @PatchMapping("/{userId}/hotels")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void updateHotelAssignments(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateHotelAssignmentsRequest request) {
        userService.updateHotelAssignments(userId, request);
    }

    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
    }

    @PatchMapping("/{userId}/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void updateEnabled(
            @PathVariable Long userId,
            @RequestParam boolean enabled) {
        userService.updateEnabled(userId, enabled);
    }
}
