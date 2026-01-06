package at.technikum_wien.rest_server.mapper;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMapperTest {

	private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

	@Test
	void toResponse_ShouldMapTagsToList() {
		Document doc = new Document();
		doc.setTags("java,spring,test");
		doc.setId(1L);
		doc.setFileName("test.pdf");

		DocumentResponse response = mapper.toResponse(doc);

		assertNotNull(response);
		assertEquals(3, response.getTags().size());
		assertTrue(response.getTags().containsAll(List.of("java", "spring", "test")));
	}

	@Test
	void mapTags_ShouldHandleEmptyAndNull() {
		assertTrue(mapper.mapTags(null).isEmpty());
		assertTrue(mapper.mapTags("  ").isEmpty());
		assertTrue(mapper.mapTags("").isEmpty());
	}

	@Test
	void mapListToString_ShouldJoinWithComma() {
		String result = mapper.mapListToString(List.of("tag1", "tag2"));
		assertEquals("tag1,tag2", result);
	}

	@Test
	void mapListToString_ShouldReturnNullForEmptyList() {
		assertNull(mapper.mapListToString(null));
		assertNull(mapper.mapListToString(List.of()));
	}

	@Test
	void updateDocumentFromDto_ShouldUpdateTags() {
		Document entity = new Document();
		UpdateDocumentRequest dto = new UpdateDocumentRequest();
		dto.setTags(List.of("new", "tags"));

		mapper.updateDocumentFromDto(dto, entity);

		assertEquals("new,tags", entity.getTags());
	}
}
