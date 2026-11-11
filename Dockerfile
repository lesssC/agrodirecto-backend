# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app

# Primero solo el wrapper y el pom: si no cambian, Docker reutiliza las dependencias descargadas.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline -DskipTests || true

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/agrodirecto-backend-*.jar /app/app.jar

# ---------- Etapa 2: ejecución (solo el JRE, imagen más liviana) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios: si alguien comprometiera la app, no sería root dentro del contenedor.
RUN groupadd --system agrodirecto && useradd --system --gid agrodirecto agrodirecto
COPY --from=compilacion --chown=agrodirecto:agrodirecto /app/app.jar app.jar
USER agrodirecto

ENV TZ=America/Lima \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
