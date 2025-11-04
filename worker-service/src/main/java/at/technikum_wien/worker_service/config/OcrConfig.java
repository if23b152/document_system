package at.technikum_wien.worker_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.io.File;

/**
 * Configures Tesseract OCR settings (e.g., path to binaries, language data).
 * In a Docker setup, tesseract is typically installed in the container image.
 */
@Configuration
public class OcrConfig {

    private static final Logger log = LoggerFactory.getLogger(OcrConfig.class);

    @Bean
    public String tesseractPath() {
        return "/usr/share/tessdata"; // or some configurable path
    }

    @Bean
    public File tempOcrDir() {
        File tempDir = new File("/tmp/ocr");
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            log.info("Created temporary OCR directory: {}", created ? tempDir.getAbsolutePath() : "failed");
        }
        return tempDir;
    }
}