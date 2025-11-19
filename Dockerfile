FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080

WORKDIR /app
ENV TZ=Asia/Seoul

COPY app.jar app-server.jar

ENTRYPOINT ["java", "-jar", "-Duser.timezone=${TZ}", "/app-server.jar", "--spring.profiles.active=${PROFILE}"]