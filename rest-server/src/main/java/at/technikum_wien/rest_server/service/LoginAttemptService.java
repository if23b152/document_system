package at.technikum_wien.rest_server.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection for the login endpoint.
 * Failed attempts are counted per username and per client IP; after
 * MAX_ATTEMPTS failures within the window further attempts are rejected.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private record Attempts(int count, Instant firstFailure) { }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username, String ip) {
        return blocked(userKey(username)) || blocked(ipKey(ip));
    }

    public void loginFailed(String username, String ip) {
        increment(userKey(username));
        increment(ipKey(ip));
    }

    public void loginSucceeded(String username, String ip) {
        attempts.remove(userKey(username));
        attempts.remove(ipKey(ip));
    }

    private boolean blocked(String key) {
        Attempts a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (a.firstFailure().plus(WINDOW).isBefore(Instant.now())) {
            attempts.remove(key);
            return false;
        }
        return a.count() >= MAX_ATTEMPTS;
    }

    private void increment(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, a) -> (a == null || a.firstFailure().plus(WINDOW).isBefore(now))
                ? new Attempts(1, now)
                : new Attempts(a.count() + 1, a.firstFailure()));
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> e.getValue().firstFailure().plus(WINDOW).isBefore(now));
        }
    }

    private static String userKey(String username) {
        return "u:" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }
}
