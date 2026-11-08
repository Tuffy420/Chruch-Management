# Multi-stage Docker build for GraceConnect Church Management Backend
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Final production runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security
RUN useradd -m -u 1001 appuser

COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENV PORT=8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
