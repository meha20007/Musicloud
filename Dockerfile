# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY musicloud-backend/musicloud-backend/mvnw .
COPY musicloud-backend/musicloud-backend/.mvn .mvn
COPY musicloud-backend/musicloud-backend/pom.xml .
COPY musicloud-backend/musicloud-backend/src src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# ---- Run stage (smaller image) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/musicloud-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
