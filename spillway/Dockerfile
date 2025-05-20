# Build stage
FROM maven:3-eclipse-temurin-24-alpine@sha256:64f4ac33351e932d931bfe62cba7fcc320317a7d27c3154b6fb120111241efb2 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:24@sha256:4563384c85adc9de147a84f68607023b399b10ab5bac23e30e022635dfbcedda
WORKDIR /app

# Install Intel Media SDK and FFmpeg with QuickSync support
RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository ppa:oibaf/graphics-drivers && \
    apt-get update && \
    apt-get install -y \
        ffmpeg \
        intel-media-va-driver-non-free \
        vainfo \
        intel-gpu-tools && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /app/content /app/temp/uploads

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/spillway?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
ENV SPRING_DATASOURCE_USERNAME=spillway
ENV SPRING_DATASOURCE_PASSWORD=spillway
ENV VIDEO_ENCODING_USE_HARDWARE_ACCELERATION=true
ENV VIDEO_ENCODING_HARDWARE_ACCELERATOR=qsv

ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8081