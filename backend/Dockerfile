FROM openjdk:21

RUN echo "Building backend..."

RUN microdnf install findutils

WORKDIR /backend
COPY . /backend
WORKDIR /backend

RUN chmod +x gradlew
RUN ./gradlew build -x test
RUN ./gradlew flywayRepair
RUN ./gradlew flywayMigrate

CMD ["java", "-XX:MaxRAMPercentage=90.0", "-jar", "./build/libs/gruzhub-0.0.1-SNAPSHOT.jar"]