FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN ./gradlew :db-migrations:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/db-migrations/build/libs/db-migrations-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
