package at.technikum_wien.rest_server.model;

public record AuthResponse(Long id, String username, String email, UserRole role, boolean temporary) {

    public static AuthResponse fromUser(AppUser user) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getTemporary())
        );
    }
}
