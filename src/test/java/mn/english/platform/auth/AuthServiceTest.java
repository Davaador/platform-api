package mn.english.platform.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mn.english.platform.auth.dto.LoginRequest;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService users;

    @Mock
    private TokenService tokens;

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, tokens);
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
    void loginWithTheCorrectPasswordReturnsAToken() {
        User user = new User(
                "bat@example.com", passwordEncoder.encode("StrongPass123"), "Bat", Role.USER);
        TokenResponse expected = new TokenResponse("token", "Bearer", 900);
        when(users.findByEmail("bat@example.com")).thenReturn(Optional.of(user));
        when(tokens.issue(user)).thenReturn(expected);

        TokenResponse actual =
                authService.login(new LoginRequest("bat@example.com", "StrongPass123"));

        assertSame(expected, actual);
    }

    @Test
    void loginWithAWrongPasswordFails() {
        User user = new User(
                "bat@example.com", passwordEncoder.encode("StrongPass123"), "Bat", Role.USER);
        when(users.findByEmail("bat@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("bat@example.com", "WrongPassword1")));
        verifyNoInteractions(tokens);
    }

    @Test
    void loginWithAnUnknownEmailFails() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "StrongPass123")));
        verifyNoInteractions(tokens);
    }
}
