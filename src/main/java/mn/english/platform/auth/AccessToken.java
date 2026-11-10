package mn.english.platform.auth;

/** A signed JWT and how long it is valid. */
public record AccessToken(String value, long expiresInSeconds) {}
