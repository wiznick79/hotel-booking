package pt.hotelbooking.identity.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import pt.hotelbooking.identity.auth.repository.IdentityUserRepository;
import pt.hotelbooking.identity.auth.service.AuthenticationTokenService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import pt.hotelbooking.identity.auth.model.dto.CustomerRegistrationRequest;
import pt.hotelbooking.identity.auth.model.dto.EmailVerificationRequest;
import pt.hotelbooking.identity.auth.service.IdentityUserService;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final IdentityUserRepository userRepository;

    private final AuthenticationTokenService authenticationTokenService;

    private final IdentityUserService identityUserService;

    @Value("${auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${auth.refresh-token.expires-in-days:14}")
    private long refreshTokenExpiresInDays;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        return responseWithRefreshCookie(authenticationTokenService.issueTokens(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "hotel_booking_refresh", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required.");
        }

        return responseWithRefreshCookie(authenticationTokenService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "hotel_booking_refresh", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authenticationTokenService.revoke(refreshToken);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody CustomerRegistrationRequest request) {
        identityUserService.registerCustomer(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<TokenResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        return responseWithRefreshCookie(identityUserService.verifyCustomerEmail(request));
    }

    private ResponseEntity<TokenResponse> responseWithRefreshCookie(
            AuthenticationTokenService.IssuedTokens issuedTokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(issuedTokens.refreshToken()).toString())
                .body(issuedTokens.response());
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from("hotel_booking_refresh", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenExpiresInDays))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from("hotel_booking_refresh", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    public record LoginRequest(String username, String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
