package mn.english.platform.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import mn.english.platform.config.LoginLimitProperties;
import org.springframework.stereotype.Component;

/**
 * Counts failed logins per email and per IP address (in memory).
 * Limits:
 * - one email: protects an account when many IPs attack it
 * - one IP: protects many accounts when one IP tries many emails
 *
 * Note: the data lives in memory, so it resets when the app restarts and is not shared
 * between several app instances. Use Redis later if we run more than one instance.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_KEYS = 10_000;

    private final LoginLimitProperties properties;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public LoginAttemptService(LoginLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Throws if this email or this IP has too many recent failures. */
    public synchronized void check(String email, String ip) {
        Instant now = clock.instant();
        long waitEmail = waitSeconds(emailKey(email), properties.maxFailuresPerEmail(), now);
        long waitIp = waitSeconds(ipKey(ip), properties.maxFailuresPerIp(), now);
        long wait = Math.max(waitEmail, waitIp);
        if (wait > 0) {
            throw new TooManyLoginAttemptsException(wait);
        }
    }

    public synchronized void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        if (failures.size() > MAX_KEYS) {
            prune(now);
        }
        failures.computeIfAbsent(emailKey(email), k -> new ArrayDeque<>()).addLast(now);
        failures.computeIfAbsent(ipKey(ip), k -> new ArrayDeque<>()).addLast(now);
    }

    /** A correct login clears the failures for that email (not for the IP). */
    public synchronized void recordSuccess(String email) {
        failures.remove(emailKey(email));
    }

    /** How long to wait (seconds), or 0 if the key is not blocked. */
    private long waitSeconds(String key, int maxFailures, Instant now) {
        Deque<Instant> times = failures.get(key);
        if (times == null) {
            return 0;
        }
        removeOld(times, now);
        if (times.size() < maxFailures) {
            return 0;
        }
        // The key is free again when enough old failures leave the window.
        Iterator<Instant> iterator = times.iterator();
        Instant deciding = iterator.next();
        for (int i = 0; i < times.size() - maxFailures; i++) {
            deciding = iterator.next();
        }
        Instant freeAt = deciding.plus(properties.window());
        long millis = Duration.between(now, freeAt).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private void removeOld(Deque<Instant> times, Instant now) {
        Instant limit = now.minus(properties.window());
        while (!times.isEmpty() && !times.peekFirst().isAfter(limit)) {
            times.pollFirst();
        }
    }

    private void prune(Instant now) {
        failures.values().forEach(times -> removeOld(times, now));
        failures.values().removeIf(Deque::isEmpty);
    }

    private static String emailKey(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return "email:" + value;
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null ? "unknown" : ip);
    }
}
