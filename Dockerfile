# 构建阶段
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/marsreg-document/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 