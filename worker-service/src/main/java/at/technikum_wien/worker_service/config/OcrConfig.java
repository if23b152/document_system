package at.technikum_wien.worker_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.io.File;

/**
 * Spring configuration class for everything the OCR process needs.
 * It prepares:
 *  - The path to Tesseract's language data files
 *  - A temporary folder where PDFs are stored before OCR runs
 */
@Configuration // Tells Spring: this class provides configuration beans
public class OcrConfig {

    // Logger for printing configuration-related messages
    private static final Logger log = LoggerFactory.getLogger(OcrConfig.class);

    // Path to the folder that contains Tesseract language files (tessdata)
    // This value is read from an environment variable or application config.
    // If nothing is set, it defaults to "/usr/share/tessdata" (common in Linux/Docker).
    @Value("${TESSDATA_PATH:/usr/share/tessdata}")
    private String tesseractDataPath;

    /**
     * Exposes the Tesseract data path as a Spring bean.
     * Other classes (like the OCR engine) can inject this value and know
     * where Tesseract's language files are located.
     */
    @Bean // Registers this return value as a Spring-managed object
    public String tesseractPath() {
        // Log which path is being used
        log.info("Configuring Tesseract data path to: {}", tesseractDataPath);

        // Return the path so Spring can inject it where needed
        return tesseractDataPath;
    }

    /**
     * Creates (if necessary) and provides a temporary directory for OCR processing.
     * The worker first downloads the PDF and saves it to disk,
     * then Tesseract reads the file from this directory.
     */
    @Bean // Makes this directory available as an injectable bean
    public File tempOcrDir() {
        // Define the folder inside the container where temp files will be stored
        File tempDir = new File("/tmp/ocr");

        // If the folder does not exist yet, create it
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();

            // Log whether creation worked or not
            log.info("Created temporary OCR directory: {}",
                    created ? tempDir.getAbsolutePath() : "failed");
        }

        // Return the folder so other services can use it
        return tempDir;
    }
}