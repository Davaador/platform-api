package mn.english.platform.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void rejectsASecretShorterThan32Bytes() {
        assertThrows(
                IllegalStateException.class,
                () -> new JwtProperties("too-short", "english-platform", Duration.ofMinutes(15)));
    }

    @Test
    void rejectsAMissingSecret() {
        assertThrows(
                IllegalStateException.class,
                () -> new JwtProperties(null, "english-platform", Duration.ofMinutes(15)));
    }

    @Test
    void acceptsALongSecret() {
        assertDoesNotThrow(() -> new JwtProperties(
                "a-long-secret-that-has-more-than-32-bytes", "english-platform", Duration.ofMinutes(15)));
    }
}
