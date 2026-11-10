package mn.english.platform.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Fail fast: the app must not start with a weak or missing secret.
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes long");
        }
    }
}
