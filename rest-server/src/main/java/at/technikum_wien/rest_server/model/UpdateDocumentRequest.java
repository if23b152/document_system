package at.technikum_wien.rest_server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentRequest {

    // New file name (must not be empty)
    @NotBlank(message = "File name cannot be blank")
    @Size(max = 255, message = "File name is too long")
    private String fileName;

    // Manually edited or AI-generated summary
    @Size(max = 10000, message = "Summary is too long")
    private String summary;

    // List of tags (accepts either single value or array in JSON)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Size(max = 50, message = "Too many tags")
    private List<@Size(max = 50, message = "Tag is too long") String> tags;
}