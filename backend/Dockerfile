FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy pre-built JAR (already compiled - faster than rebuilding in container)
COPY target/task-hub-backend-1.0.0.jar app.jar

# Also copy schema.sql for reference (actual init done via existing MySQL)
COPY src/main/resources/schema.sql /app/schema.sql

EXPOSE 8096

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
