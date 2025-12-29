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

    @NotBlank(message = "File name cannot be blank")
    private String fileName;

    private String summary; // optional

    /*
    The problem was a data type mismatch where the frontend sent a plain JSON string (e.g., "hello") for the tags, but
    the backend expected a JSON array (["hello"]), causing Jackson to fail during deserialization. We solved it by using
     the @JsonFormat annotation to allow single-value-as-array conversion, which instructs Spring to automatically wrap
     a single string into a list if necessary.
     */
    // This is the magic fix for the "Cannot construct instance of ArrayList" error
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> tags;
}
