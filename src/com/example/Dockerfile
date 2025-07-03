FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY . .

RUN apk add --no-cache bash && \
    chmod +x compile.sh && \
    ./compile.sh

EXPOSE 8080

CMD ["java", "-cp", "bin:lib/mysql-connector-j-9.2.0.jar", "com.example.Server"]
