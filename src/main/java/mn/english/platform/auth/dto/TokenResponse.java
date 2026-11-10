package mn.english.platform.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}
