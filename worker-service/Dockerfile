# Multi-stage Dockerfile to build and run the worker-service efficiently

# --- Stage 1: Build Stage (builder) ---
# Use Java 21 JDK to compile the project
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven configuration first (for dependency caching)
COPY pom.xml .

# Copy all source code and Maven wrapper files
COPY src src
COPY mvnw ./
COPY .mvn .mvn

# Build the application and skip tests (faster build, no DB needed here)
RUN ./mvnw clean install -DskipTests

# --- Stage 2: Runtime Stage ---
# Use a smaller Java runtime image (JRE only, no compiler)
FROM eclipse-temurin:21-jre-jammy

# Set working directory for the runtime container
WORKDIR /app

# Install Tesseract OCR and required libraries (for image/PDF processing)
# - Update package list
# - Install required tools and libraries:
#     wget: used to download files
#     tesseract-ocr: main OCR engine
#     tesseract-ocr-eng: English language data
#     ghostscript: handles PDFs and PostScript files
#     libtiff5, libjpeg-turbo8, libpng16-16: support common image formats
# - Clean up to reduce image size
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        wget \
        tesseract-ocr \
        tesseract-ocr-eng \
        ghostscript \
        libtiff5 \
        libjpeg-turbo8 \
        libpng16-16 && \
    rm -rf /var/lib/apt/lists/*

# Download fast English OCR training data for Tesseract
RUN mkdir -p /usr/share/tessdata && \
    wget -O /usr/share/tessdata/eng.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata

# Set environment variables for Tesseract
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV LD_LIBRARY_PATH=/usr/lib:/usr/local/lib

# Expose port 8082 for the worker-service
EXPOSE 8082

# Copy the built JAR file from the builder stage into this container
COPY --from=builder /app/target/worker-service-0.0.1-SNAPSHOT.jar worker-service.jar

# Run the Spring Boot worker-service
ENTRYPOINT ["java", "-jar", "worker-service.jar"]
