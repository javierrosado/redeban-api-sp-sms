# ---- Build stage (UBI + JDK 17 + Maven) -------------------------------------
FROM registry.access.redhat.com/ubi8/openjdk-17:latest AS build
USER root
# Instala Maven desde repos UBI (evitas Docker Hub)
RUN microdnf -y update && microdnf -y install maven && microdnf -y clean all
WORKDIR /workspace
COPY . .
# Si quieres ver versión exacta de Maven (debug):
# RUN mvn -v
RUN mvn -q -DskipTests clean package

# ---- Runtime stage (Red Hat UBI OpenJDK 17) ---------------------------------
FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:latest
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
WORKDIR /work/
COPY --from=build /workspace/target/*-runner.jar /work/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/work/app.jar"]

