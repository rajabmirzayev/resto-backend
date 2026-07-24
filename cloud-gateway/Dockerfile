FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN ./gradlew :cloud-gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/cloud-gateway/build/libs/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
