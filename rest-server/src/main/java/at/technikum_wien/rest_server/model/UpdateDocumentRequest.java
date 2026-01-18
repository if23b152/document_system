package at.technikum_wien.rest_server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
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
    private String fileName;

    // Manually edited or AI-generated summary
    private String summary;

    // List of tags (accepts either single value or array in JSON)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> tags;
}