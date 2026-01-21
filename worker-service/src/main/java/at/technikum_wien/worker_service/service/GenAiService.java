package at.technikum_wien.worker_service.service;

import com.google.genai.*;
import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for calling the Generative AI (Google Gemini)
 * to create a summary from the OCR-extracted text.
 */
@Service
public class GenAiService {

    // Logger for GenAI-related messages
    private static final Logger log = LoggerFactory.getLogger(GenAiService.class);

    // Client provided by the Google GenAI SDK to talk to the API
    private final Client genAiClient;

    // Name of the model to use (e.g. "gemini-pro")
    private final String modelName;

    // Constructor injection of the GenAI client and the model name
    public GenAiService(Client genAiClient, String genAiModelName) {
        this.genAiClient = genAiClient;
        this.modelName = genAiModelName;
    }

    /**
     * Sends the given text to Google Gemini and returns a generated summary.
     *
     * @param text The text extracted by OCR.
     * @return The generated summary.
     */
    public String generateSummary(String text) {
        try {
            // Log how much text is being sent to the AI
            log.info("Sending text to GenAI for summarization. Text length: {}", text.length());

            // Build a simple prompt for the AI model
            String prompt = "Summarize the following text in a concise paragraph:\n\n" + text;

            // Call the GenAI API using the selected model
            GenerateContentResponse response =
                    genAiClient.models.generateContent(
                            modelName, // which model to use
                            prompt,    // what we want the AI to do
                            null       // no extra parameters for now
                    );

            // Extract the text response from the API result
            String summary = response.text();

            // Log result length for debugging and monitoring
            log.info("GenAI summarization successful. Summary length: {}",
                    summary != null ? summary.length() : 0);

            return summary;

        } catch (Exception e) {
            // Catch and wrap any API, network, or SDK error
            log.error("GenAI request failed: {}", e.getMessage(), e);
            throw new RuntimeException("GenAI request failed: " + e.getMessage(), e);
        }
    }
}