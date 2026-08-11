# ==============================================================================
# Ultra-fast Dockerfile for Reshaemo Test Platform (Spring Boot + Java 21)
# ==============================================================================

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create storage directory for uploaded files/images
RUN mkdir -p /app/storage/images /app/storage/db

# Volume mount for data persistence
VOLUME /app/storage

# Expose HTTP port
EXPOSE 8080

# Copy compiled JAR
COPY build/libs/*.jar app.jar

# Run Application
ENTRYPOINT ["java", "-Duser.timezone=Europe/Moscow", "-jar", "app.jar"]
