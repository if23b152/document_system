package at.technikum_wien.rest_server.mapper;

import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring") // MapStruct mapper managed as a Spring Bean
public interface DocumentMapper {

    // 1. Converts Entity -> DTO (used for API responses)
    @Mapping(target = "ownerUsername", expression = "java(document.getOwner() != null ? document.getOwner().getUsername() : null)")
    DocumentResponse toResponse(Document document);

    // 2. Updates existing Entity from DTO (used for UPDATE requests)
    @Mapping(target = "tags", expression = "java(mapListToString(dto.getTags()))")
    void updateDocumentFromDto(UpdateDocumentRequest dto, @MappingTarget Document entity);

    // Helper method: Converts comma-separated String -> List<String> (Entity to DTO)
    default List<String> mapTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .toList();
    }

    // Helper method: Converts List<String> -> comma-separated String (DTO to Entity)
    default String mapListToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags);
    }
}
