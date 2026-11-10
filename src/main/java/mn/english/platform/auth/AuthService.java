package mn.english.platform.auth;

import java.util.Optional;
import mn.english.platform.auth.dto.LoginRequest;
import mn.english.platform.auth.dto.RefreshRequest;
import mn.english.platform.auth.dto.RegisterRequest;
import mn.english.platform.auth.dto.TokenResponse;
import mn.english.platform.user.Role;
import mn.english.platform.user.User;
import mn.english.platform.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptService loginAttempts;

    public AuthService(
            UserService users,
            PasswordEncoder passwordEncoder,
            TokenService tokens,
            RefreshTokenService refreshTokens,
            LoginAttemptService loginAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
    }

    public User register(RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        return users.create(request.email(), hash, request.displayName(), Role.USER);
    }

    public TokenResponse login(LoginRequest request, String clientIp) {
        // Check first, so a blocked client cannot even test a password.
        loginAttempts.check(request.email(), clientIp);

        Optional<User> user = users.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()));
        if (user.isEmpty()) {
            // We count unknown emails too, so attackers learn nothing about which emails exist.
            loginAttempts.recordFailure(request.email(), clientIp);
            throw new InvalidCredentialsException();
        }

        loginAttempts.recordSuccess(request.email());
        User found = user.get();
        return response(tokens.issue(found), refreshTokens.issue(found.getId()));
    }

    public TokenResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(request.refreshToken());
        User user = users.getById(rotation.userId());
        return response(tokens.issue(user), rotation.rawToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    private static TokenResponse response(AccessToken access, String refreshToken) {
        return new TokenResponse(access.value(), refreshToken, "Bearer", access.expiresInSeconds());
    }
}
