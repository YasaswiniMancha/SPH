FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace

COPY . .
RUN test -n "$MODULE"
RUN mvn -B -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app

COPY --from=build /workspace/${MODULE}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
