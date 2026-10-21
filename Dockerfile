# Build: compila com o Maven Wrapper do projeto.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar app.jar

# Runtime: só o JRE, usuário sem privilégios.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/app.jar app.jar
USER app

# Ajustado para hospedagens gratuitas (~512 MB): heap proporcional ao limite do container,
# SerialGC (menor consumo de memória) e menos threads de compilação.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -Xss512k"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT:-${PORT:-8080}}/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
