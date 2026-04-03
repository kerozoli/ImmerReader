FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

FROM eclipse-temurin:21-jre-alpine
RUN mkdir -p /data
VOLUME /data
COPY --from=builder /app/target/immerreader-1.0.0.jar immerreader-1.0.0.jar

ENTRYPOINT ["java","-jar","/immerreader-1.0.0.jar"]