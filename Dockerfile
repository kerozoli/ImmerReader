FROM openjdk:17-alpine
COPY target/immerreader-1.0.2.jar immerreader-1.0.2.jar
ENTRYPOINT ["java","-jar","/immerreader-1.0.2.jar"]