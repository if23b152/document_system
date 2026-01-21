package at.technikum_wien.worker_service.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring configuration class for the Generative AI (Google Gemini) client.
 * It creates and configures the API client and provides the model name.
 */
@Configuration
public class GenAiConfig {

    // Logger for configuration-related messages
    private static final Logger log = LoggerFactory.getLogger(GenAiConfig.class);

    // Name of the Gemini model to use (from application.properties)
    @Value("${gemini.model.name}")
    private String modelName;

    /**
     * Creates and configures the GenAI client using the API key.
     */
    @Bean
    public Client genAiClient(@Value("${gemini.api.key}") String apiKey) {
        log.info("Initializing Google GenAI Client for model: {}", modelName);

        // Simple safety check to verify the API key is actually loaded
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("CRITICAL: API Key is NULL or EMPTY in GenAiConfig!");
        } else {
            log.info("API Key is present (length: {})", apiKey.length());
        }

        // Build and return the GenAI client with the API key
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Exposes the model name as a Spring bean so it can be injected into services.
     */
    @Bean
    public String genAiModelName() {
        return modelName;
    }
}