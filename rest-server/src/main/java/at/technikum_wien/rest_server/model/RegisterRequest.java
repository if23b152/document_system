package at.technikum_wien.rest_server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, '_', '.' and '-'.")
        String username,
        @NotBlank
        @Email
        @Size(max = 120)
        String email,
        @NotBlank
        // BCrypt only uses the first 72 bytes of a password
        @Size(min = 8, max = 72)
        String password
) {
}
