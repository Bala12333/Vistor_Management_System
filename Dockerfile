# Stage 1: Build the Java application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the backend source code
COPY backend /app/backend

# Navigate to the backend directory and build the JAR
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Stage 2: Run the Java application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/backend/target/vms-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
