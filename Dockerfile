FROM gradle:8.5-jdk21 AS builder
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .

RUN chmod +x ./gradlew

RUN ./gradlew build --no-daemon -x test
FROM eclipse-temurin:21-jre AS final
COPY build/libs/*.jar app.jar

EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]
