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

# --- Stage 2: Runtime Stage (runner) ---
# Use a minimal JRE image (Java Runtime Environment) for the final production container.
# This significantly reduces the image size and attack surface.
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Expose the internal container port (8082, as set in worker-service/application.properties)
# This is primarily documentation; the actual port publishing is handled by docker-compose.yml.
EXPOSE 8082

# Copy the built JAR file from the 'builder' stage into this final, small image.
# We assume the JAR is named based on the artifactId: worker-service.jar
COPY --from=builder /app/target/worker-service-0.0.1-SNAPSHOT.jar worker-service.jar

# Define the command to run the Spring Boot application.
# The application will automatically use the environment variables
# (SPRING_RABBITMQ_HOST, SPRING_DATASOURCE_URL, etc.) passed via docker-compose.yml.
ENTRYPOINT ["java", "-jar", "worker-service.jar"]