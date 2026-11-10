package mn.english.platform.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import mn.english.platform.config.RefreshTokenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, rotates and revokes refresh tokens.
 * Rotation: every refresh token can be used only once. Using it returns a new one.
 * Reuse detection: if an old (already used) token comes back, someone may have stolen it,
 * so we revoke the whole family and the user must log in again.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository repository, RefreshTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** The new raw token (send it to the client) and the owner of the token. */
    public record Rotation(String rawToken, UUID userId) {}

    /** Starts a new family. Used after a successful login. */
    @Transactional
    public String issue(UUID userId) {
        return create(userId, UUID.randomUUID());
    }

    /**
     * Exchanges a valid refresh token for a new one.
     * noRollbackFor: when we detect reuse we revoke the family AND throw an exception.
     * Without it the exception would roll back the revoke.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for user {}; family revoked",
                    current.getUserId());
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        current.revoke(now);
        String next = create(current.getUserId(), current.getFamilyId());
        return new Rotation(next, current.getUserId());
    }

    /** Logout: revokes the whole family. Unknown tokens are ignored. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private String create(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        repository.save(new RefreshToken(userId, familyId, hash(raw), now, now.plus(properties.ttl())));
        return raw;
    }

    /**
     * SHA-256 is fine here (BCrypt is not needed): the token is 32 random bytes,
     * so it cannot be guessed, and a fast hash lets us find it in the database.
     */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
