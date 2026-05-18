FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw || true

RUN ./mvnw clean install || mvn clean install

EXPOSE 8080

CMD ["java", "-jar", "target/expense-tracker-backend-0.0.1-SNAPSHOT.jar"]
