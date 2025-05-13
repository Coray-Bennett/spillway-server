# Build stage
FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17
RUN apt-get update -qq && apt-get install -y ffmpeg
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY content content

ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/spillway?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
ENV SPRING_DATASOURCE_USERNAME=spillway
ENV SPRING_DATASOURCE_PASSWORD=spillway

ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8081