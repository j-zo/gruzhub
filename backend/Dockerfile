FROM openjdk:21


RUN echo "Building backend..."


# Declare build-time arguments
ARG DB_URL
ARG DB_USERNAME
ARG DB_PASSWORD

# Optional: expose them as ENV variables in the image
ENV DB_URL=$DB_URL
ENV DB_USERNAME=$DB_USERNAME
ENV DB_PASSWORD=$DB_PASSWORD

RUN microdnf install findutils

WORKDIR /backend
COPY . /backend
WORKDIR /backend

RUN chmod +x gradlew
RUN ./gradlew build -x test
RUN ./gradlew flywayRepair
RUN ./gradlew flywayMigrate

CMD ["java", "-XX:MaxRAMPercentage=90.0", "-jar", "./build/libs/gruzhub-0.0.1-SNAPSHOT.jar"]