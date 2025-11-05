package at.technikum_wien.worker_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.io.File;

/**
 * Configuration class for Tesseract OCR settings.
 * Sets up paths and directories needed for OCR processing in the worker service.
 * In Docker, Tesseract is usually installed inside the container image,
 * so these paths must match the installed location.
 */
@Configuration // Marks this class as a Spring configuration class
public class OcrConfig {

    // Logger for printing messages during configuration
    private static final Logger log = LoggerFactory.getLogger(OcrConfig.class);

    // Path to the Tesseract "tessdata" directory (contains language training files)
    // Uses an environment variable TESSDATA_PATH if available, otherwise defaults to /usr/share/tessdata
    @Value("${TESSDATA_PATH:/usr/share/tessdata}")
    private String tesseractDataPath;

    /**
     * Provides the Tesseract data path as a Spring bean.
     * This bean is injected into OcrService so it knows where to find language data.
     */
    @Bean // Marks this method as a bean provider
    public String tesseractPath() {
        log.info("Configuring Tesseract data path (Classic Tesseract Path) to: {}", tesseractDataPath);
        return tesseractDataPath;
    }

    /**
     * Creates a temporary directory for OCR processing.
     * OCR engine needs a file on disk, so input streams are saved here before processing.
     */
    @Bean
    public File tempOcrDir() {
        File tempDir = new File("/tmp/ocr"); // Path inside the container
        if (!tempDir.exists()) {             // Create directory if it doesn’t exist
            boolean created = tempDir.mkdirs();
            log.info("Created temporary OCR directory: {}", created ? tempDir.getAbsolutePath() : "failed");
        }
        return tempDir; // Return the directory as a Spring bean
    }
}
