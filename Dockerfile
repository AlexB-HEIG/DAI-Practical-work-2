# Base image
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the jar file
COPY target/DAI-Practical-work-2-1.0-SNAPSHOT.jar /app/DAI-Practical-work-2-1.0-SNAPSHOT.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "DAI-Practical-work-2-1.0-SNAPSHOT.jar"]

# Set the default command
CMD ["--help"]