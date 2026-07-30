FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN ./gradlew :role-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/role-service/build/libs/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
