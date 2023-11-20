FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

FROM openjdk:17-alpine
COPY --from=builder /app/target/immerreader-1.0.0.jar immerreader-1.0.0.jar

ENTRYPOINT ["java","-jar","/immerreader-1.0.0.jar"]