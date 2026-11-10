package mn.english.platform.user;

import java.util.UUID;

/** What we send to the client. It never contains the password hash. */
public record UserResponse(UUID id, String email, String displayName, Role role, String level) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getLevel());
    }
}
