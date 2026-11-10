package mn.english.platform.user;

import mn.english.platform.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin user when the app starts, using ADMIN_EMAIL and ADMIN_PASSWORD.
 * Nobody can become an admin through the public register endpoint.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminProperties properties;
    private final UserService users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            AdminProperties properties, UserService users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.email();
        String password = properties.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("No admin credentials configured, skipping admin bootstrap");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 12 characters long");
        }
        if (users.findByEmail(email).isPresent()) {
            return;
        }
        users.create(email, passwordEncoder.encode(password), "Administrator", Role.ADMIN);
        log.info("Admin user created");
    }
}
