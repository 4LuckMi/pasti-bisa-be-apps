FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-ind tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring
WORKDIR /app
COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/storage && chown -R spring:spring /app
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
