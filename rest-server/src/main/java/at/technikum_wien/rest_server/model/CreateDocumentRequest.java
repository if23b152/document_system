package at.technikum_wien.rest_server.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotNull(message = "File must not be null")
    private MultipartFile file;
}
