package mn.english.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import mn.english.platform.config.RefreshTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new RefreshTokenService(
                repository, new RefreshTokenProperties(Duration.ofDays(30)), fixedClock);
    }

    private RefreshToken stored(String raw, Instant expiresAt) {
        return new RefreshToken(
                UUID.randomUUID(), UUID.randomUUID(),
                RefreshTokenService.hash(raw), NOW.minusSeconds(60), expiresAt);
    }

    @Test
    void issueStoresOnlyTheHashOfTheToken() {
        UUID userId = UUID.randomUUID();

        String raw = service.issue(userId);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertNotEquals(raw, saved.getValue().getTokenHash());
        assertEquals(RefreshTokenService.hash(raw), saved.getValue().getTokenHash());
        assertEquals(userId, saved.getValue().getUserId());
        assertEquals(NOW.plus(Duration.ofDays(30)), saved.getValue().getExpiresAt());
    }

    @Test
    void rotateRevokesTheOldTokenAndKeepsTheFamily() {
        RefreshToken current = stored("old", NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(RefreshTokenService.hash("old")))
                .thenReturn(Optional.of(current));

        RefreshTokenService.Rotation rotation = service.rotate("old");

        assertEquals(NOW, current.getRevokedAt());
        assertNotEquals("old", rotation.rawToken());
        assertEquals(current.getUserId(), rotation.userId());
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertEquals(current.getFamilyId(), saved.getValue().getFamilyId());
    }

    @Test
    void rotateFailsForAnUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("unknown"));
    }

    @Test
    void rotateFailsForAnExpiredToken() {
        RefreshToken expired = stored("old", NOW.minusSeconds(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("old")))
                .thenReturn(Optional.of(expired));

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("old"));
        assertNull(expired.getRevokedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void reusingAnOldTokenRevokesTheWholeFamily() {
        RefreshToken used = stored("old", NOW.plus(Duration.ofDays(1)));
        used.revoke(NOW.minusSeconds(30));
        when(repository.findByTokenHash(RefreshTokenService.hash("old")))
                .thenReturn(Optional.of(used));

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("old"));
        verify(repository).revokeFamily(used.getFamilyId(), NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void revokeRevokesTheFamily() {
        RefreshToken token = stored("refresh", NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(RefreshTokenService.hash("refresh")))
                .thenReturn(Optional.of(token));

        service.revoke("refresh");

        verify(repository).revokeFamily(token.getFamilyId(), NOW);
    }
}
