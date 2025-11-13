FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Seoul

COPY build/libs/*.jar app-server.jar

ENTRYPOINT ["java", "-jar", "-Duser.timezone=${TZ}", "/app-server.jar", "--spring.profiles.active=${PROFILE}"]