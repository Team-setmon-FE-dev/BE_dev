FROM openjdk:17-jdk-slim

# FFmpeg 및 필수 패키지 설치
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Gradle 빌드 결과물 jar 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 8080 포트 노출 및 앱 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]