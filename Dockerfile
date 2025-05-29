FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/hello-world-1.0-SNAPSHOT.jar /app/hello-world.jar

EXPOSE 8090

# Add the server.port parameter if you don't use Springboot default port 8080
CMD ["java", "-jar", "hello-world.jar", "--server.port=8090"]