FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y maven
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

FROM eclipse-temurin:25-jre-alpine
RUN mkdir -p /data
VOLUME /data
COPY --from=builder /app/target/immerreader-1.0.0.jar immerreader-1.0.0.jar

ENTRYPOINT ["java","-jar","/immerreader-1.0.0.jar"]