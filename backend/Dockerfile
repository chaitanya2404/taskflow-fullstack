# ---------------------------------------------------------------------------
# Stage 1 - build the jar with Maven on JDK 25.
#
# pom.xml is copied and resolved on its own first so that the (slow) dependency
# download lands in a layer that only changes when the pom does. Editing Java
# source then rebuilds from the cached dependency layer.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - runtime on a JRE-only base.
#
# Nothing from the build context reaches this image: the only thing copied out
# of the build stage is the jar itself. No Maven, no JDK, no sources.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime

# curl is here purely for the container healthcheck below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user rather than root.
RUN groupadd --system --gid 1001 taskflow \
    && useradd --system --uid 1001 --gid taskflow --no-create-home --shell /usr/sbin/nologin taskflow

WORKDIR /app

COPY --from=build --chown=taskflow:taskflow /build/target/*.jar /app/app.jar

USER taskflow

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Backs the docker-compose dependency ordering and makes `docker ps` meaningful.
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
