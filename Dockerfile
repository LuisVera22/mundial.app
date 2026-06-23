# syntax=docker/dockerfile:1

# ============================================================
# Etapa 1 · Build (Maven + JDK 21)
# Copiamos primero el pom para cachear la descarga de dependencias:
# si solo cambia src/, el build reutiliza esa capa y es mucho más rápido.
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Extrae el jar en capas (dependencias / loader / app) para que la imagen
# final solo invalide la capa de aplicación en cada deploy.
RUN cp target/*.jar app.jar \
 && java -Djarmode=layertools -jar app.jar extract --destination extracted

# ============================================================
# Etapa 2 · Runtime (solo JRE 21, usuario sin privilegios)
# ============================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuario no-root
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# Capas del jar: de la más estable a la más cambiante
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# Memoria consciente del contenedor; ajustable con JAVA_OPTS en el host.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
