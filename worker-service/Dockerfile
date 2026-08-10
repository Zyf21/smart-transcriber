FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Устанавливаем FFmpeg внутрь Alpine-контейнера
RUN apk update && apk add --no-cache ffmpeg

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]