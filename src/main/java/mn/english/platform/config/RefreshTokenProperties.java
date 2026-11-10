package mn.english.platform.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(Duration ttl) {

    public RefreshTokenProperties {
        if (ttl == null) {
            ttl = Duration.ofDays(30);
        }
    }
}
