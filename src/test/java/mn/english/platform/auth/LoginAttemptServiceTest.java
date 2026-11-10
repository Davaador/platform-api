package mn.english.platform.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import mn.english.platform.config.LoginLimitProperties;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    /** A clock we can move forward in tests, so we never have to wait. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-10T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    private LoginAttemptService service(int perEmail, int perIp) {
        return new LoginAttemptService(
                new LoginLimitProperties(perEmail, perIp, Duration.ofMinutes(15)), clock);
    }

    @Test
    void blocksAnEmailAfterTooManyFailuresFromAnyIp() {
        LoginAttemptService service = service(3, 100);

        service.recordFailure("bat@example.com", "1.1.1.1");
        service.recordFailure("bat@example.com", "2.2.2.2");
        service.recordFailure("bat@example.com", "3.3.3.3");

        assertThrows(TooManyLoginAttemptsException.class,
                () -> service.check("bat@example.com", "9.9.9.9"));
        assertDoesNotThrow(() -> service.check("other@example.com", "9.9.9.9"));
    }

    @Test
    void blocksAnIpAfterTooManyFailuresForAnyEmail() {
        LoginAttemptService service = service(100, 3);

        service.recordFailure("a@example.com", "1.1.1.1");
        service.recordFailure("b@example.com", "1.1.1.1");
        service.recordFailure("c@example.com", "1.1.1.1");

        assertThrows(TooManyLoginAttemptsException.class,
                () -> service.check("new@example.com", "1.1.1.1"));
        assertDoesNotThrow(() -> service.check("new@example.com", "2.2.2.2"));
    }

    @Test
    void allowsLoginAgainAfterTheWindow() {
        LoginAttemptService service = service(3, 100);
        for (int i = 0; i < 3; i++) {
            service.recordFailure("bat@example.com", "1.1.1.1");
        }

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertDoesNotThrow(() -> service.check("bat@example.com", "1.1.1.1"));
    }

    @Test
    void retryAfterIsWhenTheOldestFailureLeavesTheWindow() {
        LoginAttemptService service = service(3, 100);
        service.recordFailure("bat@example.com", "1.1.1.1");
        clock.advance(Duration.ofMinutes(5));
        service.recordFailure("bat@example.com", "1.1.1.1");
        clock.advance(Duration.ofMinutes(5));
        service.recordFailure("bat@example.com", "1.1.1.1");

        TooManyLoginAttemptsException error = assertThrows(TooManyLoginAttemptsException.class,
                () -> service.check("bat@example.com", "1.1.1.1"));

        assertEquals(300, error.getRetryAfterSeconds());
    }

    @Test
    void aSuccessfulLoginClearsTheEmailFailures() {
        LoginAttemptService service = service(3, 100);
        service.recordFailure("bat@example.com", "1.1.1.1");
        service.recordFailure("bat@example.com", "1.1.1.1");

        service.recordSuccess("bat@example.com");
        service.recordFailure("bat@example.com", "1.1.1.1");
        service.recordFailure("bat@example.com", "1.1.1.1");

        assertDoesNotThrow(() -> service.check("bat@example.com", "1.1.1.1"));
    }

    @Test
    void emailIsCaseInsensitive() {
        LoginAttemptService service = service(2, 100);
        service.recordFailure("  Bat@Example.com ", "1.1.1.1");
        service.recordFailure("BAT@EXAMPLE.COM", "1.1.1.1");

        assertThrows(TooManyLoginAttemptsException.class,
                () -> service.check("bat@example.com", "1.1.1.1"));
    }
}
