# ---- Build stage ------------------------------------------------------------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests clean package

# ---- Runtime stage (Red Hat UBI OpenJDK 17) --------------------------------
FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:latest
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
WORKDIR /work/
COPY --from=build /workspace/target/*-runner.jar /work/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/work/app.jar"]
