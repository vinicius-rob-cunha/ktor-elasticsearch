./gradlew clean build -x test && \
docker build -t ktor-elasticsearch . && \
docker-compose up -d