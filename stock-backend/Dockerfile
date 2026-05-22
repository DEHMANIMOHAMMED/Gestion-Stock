FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S stockpilot && adduser -S stockpilot -G stockpilot
COPY --from=build /app/target/*.jar app.jar
RUN chown -R stockpilot:stockpilot /app
USER stockpilot

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
