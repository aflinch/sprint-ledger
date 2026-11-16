FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY tracker-common/pom.xml tracker-common/
COPY tracker-api/pom.xml tracker-api/
COPY tracker-cli/pom.xml tracker-cli/

RUN mvn -B -q -pl tracker-api -am dependency:go-offline

COPY tracker-common/src tracker-common/src
COPY tracker-api/src tracker-api/src

RUN mvn -B -q -pl tracker-api -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system tracker && useradd --system --gid tracker tracker

COPY --from=build /workspace/tracker-api/target/tracker-api-*.jar app.jar

USER tracker

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
