package at.technikum_wien.worker_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.lang.reflect.Field;

class GenAiServiceTest {

    private GenAiService genAiService; // The service we are testing
    @Mock private Client mockClient; // Mocked top-level Google AI client
    @Mock private Models mockModels; // Mocked sub-component for model operations
    @Mock private GenerateContentResponse mockResponse; // Mocked object for the API result

    private final String modelName = "gemini-pro"; // Dummy model name for the test

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this); // Initializes all objects marked with @Mock

        // Reflection: 'models' is final in the SDK, so we can't use a normal setter
        Field modelsField = Client.class.getDeclaredField("models"); // Access the 'models' field definition
        modelsField.setAccessible(true); // Bypass the 'private'/'final' visibility check
        modelsField.set(mockClient, mockModels); // Inject our mockModels into our mockClient

        genAiService = new GenAiService(mockClient, modelName); // Manually inject mocks into our service
    }

    @Test
    void generateSummary_Success() {
        // ARRANGE: Define the inputs and the expected outcome
        String input = "Input text"; // The dummy text to be summarized
        String expectedSummary = "Summary"; // The dummy result we expect back

        // Setup the mock behavior for the API call
        when(mockModels.generateContent(
                eq(modelName), // Match if the model name is correct
                anyString(), // Match any prompt string passed in
                isNull())) // Match when config is null (casted to avoid ambiguity)
                .thenReturn(mockResponse); // Return our mock response instead of calling Google

        when(mockResponse.text()).thenReturn(expectedSummary); // Tell the mock response to return our dummy string

        // ACT: Execute the method under test
        String result = genAiService.generateSummary(input); // Call the actual logic in GenAiService

        // ASSERT: Check if the results match expectations
        assertEquals(expectedSummary, result); // Verify the returned string matches our mock output
    }

    @Test
    void generateSummary_ThrowsException() {
        // ARRANGE: Setup a failure scenario
        // Tell the mock to throw an exception when the API is called
        when(mockModels.generateContent(
                anyString(), // Match any model string
                anyString(), // Match any prompt string
                any())) // Match any config (casted to avoid ambiguity)
                .thenThrow(new RuntimeException("API Error")); // Simulate a network or API crash

        // ACT & ASSERT: Verify the service handles the error correctly
        // Check that calling the service results in the expected RuntimeException
        assertThrows(RuntimeException.class, () -> genAiService.generateSummary("test"));
    }
}