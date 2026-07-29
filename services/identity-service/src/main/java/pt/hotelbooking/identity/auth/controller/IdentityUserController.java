package pt.hotelbooking.identity.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class IdentityUserController {

    private final IdentityUserService userService;

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
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.create(request));
    }

    @PatchMapping("/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRolesRequest request) {
        userService.updateRoles(userId, request);
    }

    @PatchMapping("/{userId}/hotels")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateHotelAssignments(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateHotelAssignmentsRequest request) {
        userService.updateHotelAssignments(userId, request);
    }

    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
    }

    @PatchMapping("/{userId}/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEnabled(
            @PathVariable Long userId,
            @RequestParam boolean enabled) {
        userService.updateEnabled(userId, enabled);
    }
}
