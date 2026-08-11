# ==============================================================================
# Multi-stage Dockerfile for Reshaemo Test Platform (Spring Boot + Java 21)
# ==============================================================================

# --- Stage 1: Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and configuration
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./

# Grant execution rights on Gradlew
RUN chmod +x gradlew

# Download dependencies (cache layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build production bootJar
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# --- Stage 2: Production Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create storage directory for uploaded files/images
RUN mkdir -p /app/storage/images /app/storage/db

# Volume mount for data persistence
VOLUME /app/storage

# Expose HTTP port
EXPOSE 8080

# Copy compiled JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Run Application
ENTRYPOINT ["java", "-Duser.timezone=Europe/Moscow", "-jar", "app.jar"]
