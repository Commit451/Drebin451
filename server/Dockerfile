FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY build/libs/*-all.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
