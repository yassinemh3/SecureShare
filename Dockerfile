# Use a JDK base image
FROM openjdk:17-jdk-slim

# Add a volume to store logs if needed
VOLUME /tmp

# Copy the built jar
COPY target/*.jar app.jar

# Run the jar
ENTRYPOINT ["java","-jar","/app.jar"]
