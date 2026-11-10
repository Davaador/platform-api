package mn.english.platform.auth;

import mn.english.platform.auth.dto.LoginRequest;
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

    public AuthService(UserService users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public User register(RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        return users.create(request.email(), hash, request.displayName(), Role.USER);
    }

    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokens.issue(user);
    }
}
