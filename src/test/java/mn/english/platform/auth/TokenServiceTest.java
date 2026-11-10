package mn.english.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import mn.english.platform.auth.dto.TokenResponse;
import mn.english.platform.config.JwtConfig;
import mn.english.platform.config.JwtProperties;
import mn.english.platform.user.Role;
import mn.english.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";
    private static final String ISSUER = "english-platform";

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties =
            new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(15));
    private final JwtEncoder encoder = config.jwtEncoder(properties);
    private final JwtDecoder decoder = config.jwtDecoder(properties);
    private final TokenService tokenService = new TokenService(encoder, properties);

    private User userWithId(UUID id, Role role) {
        User user = new User("bat@example.com", "hash", "Bat", role);
        // The id is normally created by the database, so we set it by hand in tests.
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String sign(JwtEncoder jwtEncoder, JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void issuedTokenContainsSubjectIssuerAndRole() {
        UUID id = UUID.randomUUID();

        TokenResponse response = tokenService.issue(userWithId(id, Role.ADMIN));
        Jwt jwt = decoder.decode(response.accessToken());

        assertEquals(id.toString(), jwt.getSubject());
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresInSeconds());
    }

    @Test
    void expiredTokenIsRejected() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now.minus(Duration.ofMinutes(10)))
                .expiresAt(now.minus(Duration.ofMinutes(5)))
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("USER"))
                .build();
        String token = sign(encoder, claims);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void tokenWithAnotherIssuerIsRejected() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("someone-else")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .subject(UUID.randomUUID().toString())
                .build();
        String token = sign(encoder, claims);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties otherProperties = new JwtProperties(
                "another-secret-another-secret-another-1", ISSUER, Duration.ofMinutes(15));
        TokenService otherService =
                new TokenService(config.jwtEncoder(otherProperties), otherProperties);

        String token = otherService.issue(userWithId(UUID.randomUUID(), Role.ADMIN)).accessToken();

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void tokenWithChangedPayloadIsRejected() {
        String token = tokenService.issue(userWithId(UUID.randomUUID(), Role.USER)).accessToken();
        String[] parts = token.split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"hacker\",\"roles\":[\"ADMIN\"]}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + fakePayload + "." + parts[2];

        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }
}
