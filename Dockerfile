# Stage 1: Build the JAR with Maven & OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built artifact from builder
COPY --from=builder /app/target/telephony-crm-core-1.0.0.jar app.jar

# Expose Web Port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]