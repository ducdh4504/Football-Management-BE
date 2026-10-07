# =========================
# Stage 1: Build
# =========================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Download dependencies trước để tận dụng Docker cache
RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests


# =========================
# Stage 2: Run
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/be.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]