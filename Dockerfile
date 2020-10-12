FROM packages.tools.infra:8444/openjdk:8-jre-alpine

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/ktor-elasticsearch-all.jar /app/ktor-elasticsearch-all.jar

WORKDIR /app

CMD ["java", "-server", "-jar", "ktor-elasticsearch-all.jar"]