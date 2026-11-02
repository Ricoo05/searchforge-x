FROM node:24-alpine AS web
WORKDIR /web
COPY web/package*.json ./
RUN npm ci --no-audit --no-fund
COPY web/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY --from=web /web/dist ./web/dist
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/searchforge-x-0.1.0-SNAPSHOT.jar /app/searchforge.jar
ENV SEARCHFORGE_BIND=0.0.0.0
RUN mkdir /data && chown 10001:10001 /data /app
USER 10001:10001
ENTRYPOINT ["java", "-Xmx512m", "-cp", "/app/searchforge.jar"]
