FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y maven
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:25-jre
RUN mkdir -p /data && apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
VOLUME /data
COPY --from=builder /app/target/immerreader-1.0.0.jar immerreader-1.0.0.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8099/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/immerreader-1.0.0.jar"]