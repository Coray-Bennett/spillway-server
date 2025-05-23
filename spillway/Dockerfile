# Build stage
FROM maven:3-eclipse-temurin-24-alpine@sha256:64f4ac33351e932d931bfe62cba7fcc320317a7d27c3154b6fb120111241efb2 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:24@sha256:24459ad9b750de00da0d7aacc9ac2caf2320349663283cc53571df65f934789d

# Install FFmpeg with necessary dependencies
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY conten[t] content

ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/spillway?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
ENV SPRING_DATASOURCE_USERNAME=spillway

ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8081