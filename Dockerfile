# syntax=docker/dockerfile:1

# ---- Build stage: compile and package with the full JDK ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build

# Download dependencies first, in their own layer: they only change when pom.xml does,
# so code-only changes rebuild in seconds
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src src
# Tests need Docker (Testcontainers) and already run in CI, so the image build skips them
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime stage: only the JRE and the app, no build tools or source code ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as an unprivileged user, never as root
RUN addgroup -S app && adduser -S app -G app
USER app

# Layers ordered from least to most often changed, so a code change only replaces the small last layer
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Size the heap from the container's memory limit instead of the host's
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
