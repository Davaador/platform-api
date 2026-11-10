package mn.english.platform.user;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The public API of the user feature. Other features use this class, not the repository. */
@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User create(String email, String passwordHash, String displayName, Role role) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException();
        }
        try {
            return users.saveAndFlush(new User(normalized, passwordHash, displayName.trim(), role));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same email at the same time: the database unique rule wins.
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return users.findById(id).orElseThrow(AccountNotFoundException::new);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
