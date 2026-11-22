package at.technikum_wien.rest_server.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a personal access token; validDays defaults to 90. */
public record CreateTokenRequest(
        @NotBlank @Size(max = 60) String name,
        @Min(1) @Max(365) Integer validDays) {
}
