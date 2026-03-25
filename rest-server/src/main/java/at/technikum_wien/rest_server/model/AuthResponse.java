package at.technikum_wien.rest_server.model;

public record AuthResponse(Long id, String username, String email, UserRole role) {

    public static AuthResponse fromUser(AppUser user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
