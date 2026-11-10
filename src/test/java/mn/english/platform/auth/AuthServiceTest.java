package mn.english.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import mn.english.platform.auth.dto.LoginRequest;
import mn.english.platform.auth.dto.RefreshRequest;
import mn.english.platform.auth.dto.RegisterRequest;
import mn.english.platform.auth.dto.TokenResponse;
import mn.english.platform.user.Role;
import mn.english.platform.user.User;
import mn.english.platform.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService users;

    @Mock
    private TokenService tokens;

    @Mock
    private RefreshTokenService refreshTokens;

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, tokens, refreshTokens);
    }

    private User savedUser(UUID id) {
        User user = new User(
                "bat@example.com", passwordEncoder.encode("StrongPass123"), "Bat", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void registerAlwaysCreatesAUserAndStoresOnlyAHash() {
        authService.register(new RegisterRequest("bat@example.com", "StrongPass123", "Bat"));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(users).create(eq("bat@example.com"), hash.capture(), eq("Bat"), eq(Role.USER));
        assertNotEquals("StrongPass123", hash.getValue());
        assertTrue(passwordEncoder.matches("StrongPass123", hash.getValue()));
    }

    @Test
    void loginReturnsAnAccessTokenAndARefreshToken() {
        UUID id = UUID.randomUUID();
        User user = savedUser(id);
        when(users.findByEmail("bat@example.com")).thenReturn(Optional.of(user));
        when(tokens.issue(user)).thenReturn(new AccessToken("access", 900));
        when(refreshTokens.issue(id)).thenReturn("refresh");

        TokenResponse response =
                authService.login(new LoginRequest("bat@example.com", "StrongPass123"));

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresInSeconds());
    }

    @Test
    void loginWithAWrongPasswordFails() {
        User user = savedUser(UUID.randomUUID());
        when(users.findByEmail("bat@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("bat@example.com", "WrongPassword1")));
        verifyNoInteractions(tokens, refreshTokens);
    }

    @Test
    void loginWithAnUnknownEmailFails() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "StrongPass123")));
        verifyNoInteractions(tokens, refreshTokens);
    }

    @Test
    void refreshReturnsANewPairOfTokens() {
        UUID id = UUID.randomUUID();
        User user = savedUser(id);
        when(refreshTokens.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.Rotation("new-refresh", id));
        when(users.getById(id)).thenReturn(user);
        when(tokens.issue(user)).thenReturn(new AccessToken("new-access", 900));

        TokenResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        authService.logout(new RefreshRequest("refresh"));

        verify(refreshTokens).revoke("refresh");
    }
}
