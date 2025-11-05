package at.technikum_wien.worker_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    // Using the classic Tesseract data path, which the Dockerfile also uses.
    @Value("${TESSDATA_PATH:/usr/share/tessdata}")
    private String tesseractDataPath;

    @Bean
    public String tesseractPath() {
        log.info("Configuring Tesseract data path (Classic Tesseract Path) to: {}", tesseractDataPath);
        return tesseractDataPath;
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