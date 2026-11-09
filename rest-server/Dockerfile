# FROM ubuntu:latest
# LABEL authors="kayam"

# ENTRYPOINT ["top", "-b"]

# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom and source code
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Run stage (JRE only - no compiler/tools needed at runtime)
FROM eclipse-temurin:21-jre

# Run as unprivileged user instead of root
RUN groupadd --system app && useradd --system --gid app --no-create-home app

# Set working directory
WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=build --chown=app:app /app/target/rest-server-0.0.1-SNAPSHOT.jar app.jar

USER app

# Expose the Spring Boot port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]