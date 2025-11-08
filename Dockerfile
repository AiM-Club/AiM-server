FROM eclipse-temurin:21-jre-alpine

EXPOSE 8080

ENV PROFILE=remote
ENV TZ=Asia/Seoul

COPY app.jar aim-server.jar

ENTRYPOINT ["java", "-jar", "-Duser.timezone=${TZ}", "/aim-server.jar", "--spring.profiles.active=${PROFILE}"]
