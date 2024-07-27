FROM eclipse-temurin:21
RUN ["apt-get", "update", "-qq"]
RUN ["apt-get", "install", "ffmpeg", "-y"]
COPY content content
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8081