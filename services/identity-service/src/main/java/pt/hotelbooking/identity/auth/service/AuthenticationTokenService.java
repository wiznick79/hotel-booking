package pt.hotelbooking.identity.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.hotelbooking.identity.auth.controller.AuthController;
import pt.hotelbooking.identity.auth.model.entity.IdentityRefreshToken;
import pt.hotelbooking.identity.auth.model.entity.IdentityUser;
import pt.hotelbooking.identity.auth.repository.IdentityRefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationTokenService {

    private final JwtEncoder jwtEncoder;
    private final PermissionService permissionService;
    private final IdentityRefreshTokenRepository refreshTokenRepository;

    @Value("${auth.access-token.expires-in-seconds:900}")
    private long accessTokenExpiresInSeconds;

    @Value("${auth.refresh-token.expires-in-days:14}")
    private long refreshTokenExpiresInDays;

    @Transactional
    public IssuedTokens issueTokens(IdentityUser user) {
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(new IdentityRefreshToken(
                hash(refreshToken),
                user,
                Instant.now().plusSeconds(refreshTokenExpiresInDays * 24 * 60 * 60)));

        return new IssuedTokens(createAccessToken(user), refreshToken);
    }

    @Transactional
    public IssuedTokens refresh(String rawRefreshToken) {
        IdentityRefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> token.isActiveAt(Instant.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid or expired."));

        refreshToken.revoke();
        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(IdentityRefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(IdentityRefreshToken::revoke);
    }

    private AuthController.TokenResponse createAccessToken(IdentityUser user) {
        Instant now = Instant.now();
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .map(GrantedAuthority.class::cast)
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiresInSeconds))
                .claim("roles", authorities.stream().map(GrantedAuthority::getAuthority).toList())
                .claim("permissions", permissionService.permissionsFor(authorities))
                .claim("hotelIds", user.getHotelIds().stream().map(Object::toString).toList())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new AuthController.TokenResponse(accessToken, "Bearer", accessTokenExpiresInSeconds);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
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

    public record IssuedTokens(AuthController.TokenResponse response, String refreshToken) {
    }
}
