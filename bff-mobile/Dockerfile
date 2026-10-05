FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/bff-mobile.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
