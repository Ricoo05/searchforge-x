FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/searchforge-x-0.1.0-SNAPSHOT.jar /app/searchforge.jar
ENV SEARCHFORGE_BIND=0.0.0.0
RUN mkdir /data && chown 10001:10001 /data /app
USER 10001:10001
ENTRYPOINT ["java", "-Xmx512m", "-cp", "/app/searchforge.jar"]
