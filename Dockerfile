# Multi-stage build
# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy configuration files
COPY pom.xml .
# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose ports: 8080 (Web/WS), 19210 (UDP Listen), 19211 (UDP Send target)
EXPOSE 8080
EXPOSE 19210/udp
EXPOSE 19211/udp

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
