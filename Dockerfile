FROM gradle:9.3.1-jdk21 AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle build.gradle ./
COPY src src
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/univerliga-crm-service-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
