# Complete application image: Angular is compiled and copied into Spring Boot's static resources.
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS service-build
WORKDIR /workspace
COPY building-point-locator-service/pom.xml ./
COPY building-point-locator-service/src ./src
COPY --from=frontend-build /frontend/dist/building-point-locator-ui/browser ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=service-build /workspace/target/building-point-locator-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
