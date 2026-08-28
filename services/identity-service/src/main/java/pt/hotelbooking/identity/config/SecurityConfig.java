package pt.hotelbooking.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import pt.hotelbooking.security.RsaKeyLoader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @Profile("!test")
    JwtEncoder jwtEncoder(
            @Value("${jwt.private-key-base64}") String privateKey,
            @Value("${jwt.public-key-base64}") String publicKey) {
        RSAKey rsaKey = new RSAKey.Builder(RsaKeyLoader.publicKey(publicKey))
                .privateKey(RsaKeyLoader.privateKey(privateKey))
                .keyID("hotel-booking-rsa-1")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    @Profile("!test")
    JwtDecoder jwtDecoder(@Value("${jwt.public-key-base64}") String publicKey) {
        return NimbusJwtDecoder.withPublicKey(RsaKeyLoader.publicKey(publicKey))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    @Profile("test")
    KeyPair testJwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create the test JWT key pair.", exception);
        }
    }

    @Bean
    @Profile("test")
    JwtEncoder testJwtEncoder(KeyPair testJwtKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) testJwtKeyPair.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) testJwtKeyPair.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    @Profile("test")
    JwtDecoder testJwtDecoder(KeyPair testJwtKeyPair) {
        return NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) testJwtKeyPair.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RoleClaimConverter());
        return converter;
    }
}
