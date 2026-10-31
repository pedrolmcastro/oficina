# syntax=docker/dockerfile:1

FROM eclipse-temurin:27-jdk-resolute AS build
WORKDIR /build

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY config/ config/
COPY src/main/ src/main/

RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM eclipse-temurin:27-jre-resolute AS runtime
WORKDIR /app

RUN groupadd --gid 10001 app \
    && useradd \
        --uid 10001 \
        --gid app \
        --no-create-home \
        --no-log-init \
        --shell /usr/sbin/nologin \
        app

COPY --from=build /build/build/libs/*.jar /app/application.jar

USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
