# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first so dependency layer is cached
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user
RUN addgroup -S recur && adduser -S recur -G recur

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

RUN chown recur:recur app.jar
USER recur

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
