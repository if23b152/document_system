package at.technikum_wien.worker_service.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class GenAiConfig {

    private static final Logger log = LoggerFactory.getLogger(GenAiConfig.class);

    @Value("${gemini.model.name}")
    private String modelName;

    @Bean
    public Client genAiClient(@Value("${gemini.api.key}") String apiKey) {
        log.info("Initializing Google GenAI Client for model: {}", modelName);

        // debug log (remove later if needed) to prove the key is reaching Java
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("CRITICAL: API Key is NULL or EMPTY in GenAiConfig!");
        } else {
            log.info("API Key is present (length: {})", apiKey.length());
        }

        // FIX: Use the Builder to explicitly set the API Key
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }

    // unüblich, direkt über @Value
    @Bean
    public String genAiModelName() {
        return modelName;
    }
}