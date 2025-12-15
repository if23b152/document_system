package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.SearchDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<Long> searchDocumentIds(String query) {

        Criteria criteria = new Criteria("ocrText").matches(query)
                .or(new Criteria("summary").matches(query))
                .or(new Criteria("fileName").matches(query));

        CriteriaQuery searchQuery = new CriteriaQuery(criteria);

        SearchHits<SearchDocument> hits =
                elasticsearchOperations.search(searchQuery, SearchDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent().getDocumentId())
                .distinct()
                .collect(Collectors.toList());
    }
}