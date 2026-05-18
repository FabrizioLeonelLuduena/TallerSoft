# Multi-stage build for Spring Cloud Gateway
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code
COPY src/ src/

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init wget

# Copy JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 tallersoft && \
    adduser -D -u 1001 -G tallersoft tallersoft && \
    chown -R tallersoft:tallersoft /app

USER tallersoft

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["/sbin/dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
