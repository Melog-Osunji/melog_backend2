FROM gradle:8.14.3-jdk17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY .env.spring .env.spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]