package mn.english.platform.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits for failed logins inside a sliding time window.
 * Example: 5 failures for one email in 15 minutes blocks that email until the oldest
 * failure is older than 15 minutes.
 */
@ConfigurationProperties(prefix = "app.login-limit")
public record LoginLimitProperties(
        Integer maxFailuresPerEmail, Integer maxFailuresPerIp, Duration window) {

    public LoginLimitProperties {
        if (maxFailuresPerEmail == null) {
            maxFailuresPerEmail = 5;
        }
        if (maxFailuresPerIp == null) {
            maxFailuresPerIp = 20;
        }
        if (window == null) {
            window = Duration.ofMinutes(15);
        }
    }
}
