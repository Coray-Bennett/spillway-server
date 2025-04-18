FROM eclipse-temurin:21
RUN ["apt-get", "update", "-qq"]
RUN ["apt-get", "install", "ffmpeg", "-y"]
COPY conten[t] content
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# The application will wait for MySQL to start
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/spillway?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
ENV SPRING_DATASOURCE_USERNAME=spillway
ENV SPRING_DATASOURCE_PASSWORD=spillway

ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8081