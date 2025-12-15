package at.technikum_wien.worker_service.service;

import com.google.genai.*;
import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GenAiService {

    private static final Logger log = LoggerFactory.getLogger(GenAiService.class);

    private final Client genAiClient;
    // direkt hier @value
    private final String modelName;

    // unüblich, direkt über @Value
    public GenAiService(Client genAiClient, String genAiModelName) {
        this.genAiClient = genAiClient;
        this.modelName = genAiModelName;
    }

    /**
     * Generates a summary of the input text using Google Gemini.
     *
     * @param text The text extracted by OCR.
     * @return The generated summary.
     * @throws RuntimeException if the GenAI request failed.
     */
    public String generateSummary(String text) {
        try {
            log.info("Sending text to GenAI for summarization. Text length: {}", text.length());

            String prompt = "Summarize the following text in a concise paragraph:\n\n" + text;

            GenerateContentResponse response =
                    genAiClient.models.generateContent(
                            modelName,
                            prompt,
                            null // No extra parameters for now
                    );
// system prompt, user prompt, text länge vorgeben

            String summary = response.text();

            log.info("GenAI summarization successful. Summary length: {}",
                    summary != null ? summary.length() : 0);

            return summary;

        } catch (Exception e) {
            log.error("GenAI request failed: {}", e.getMessage(), e);
            throw new RuntimeException("GenAI request failed: " + e.getMessage(), e);
        }
    }
}