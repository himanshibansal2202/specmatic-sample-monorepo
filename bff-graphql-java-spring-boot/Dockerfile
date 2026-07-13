FROM maven:3.9.12-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -Dskip.contract.tests=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/bff-graphql-java-spring-boot-0.0.1-SNAPSHOT.jar app.jar
ENV SUT_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
