# Multi-stage build for a compact and secure final image

# --- Stage 1: Build Stage (builder) ---
# Use an official JDK image (matching your Java 21 requirement) to compile the application.
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven pom.xml file first. This allows Docker to cache dependencies,
# speeding up subsequent builds if only the source code changes.
COPY pom.xml .

# Copy the rest of the source code
COPY src src
COPY mvnw ./
COPY .mvn .mvn

# Package the application into a single runnable JAR file.
# We skip tests (-DskipTests) here because Docker builds are often run without the full
# test environment (like a running database connection).
RUN ./mvnw clean install -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 1. Install Tesseract OCR and all necessary native libraries.
# CRITICAL FIX: Reverting to the existing '-dev' packages in Jammy,
# which reliably bring in the necessary runtime libraries (like liblept5)
# and ensure all image/PDF dependencies (tiff, jpeg, png, ghostscript) are present,
# preventing the SIGSEGV crashes seen previously.
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

# 2. Manually download and place the Tesseract 4 'fast' trained data
#    into the classic, expected directory: /usr/share/tessdata.
RUN mkdir -p /usr/share/tessdata && \
    wget -O /usr/share/tessdata/eng.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata

# 3. Set TESSDATA_PREFIX to the classic directory.
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV LD_LIBRARY_PATH=/usr/lib:/usr/local/lib

EXPOSE 8082

COPY --from=builder /app/target/worker-service-0.0.1-SNAPSHOT.jar worker-service.jar
ENTRYPOINT ["java", "-jar", "worker-service.jar"]