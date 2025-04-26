mvn clean package

cd server && docker build . -t petrbouda/jeffrey-testapp-server -f target/docker/Dockerfile.24-temurin
cd ..
cd client && docker build . -t petrbouda/jeffrey-testapp-client -f target/docker/Dockerfile.24-temurin

docker push petrbouda/jeffrey-testapp-server
docker push petrbouda/jeffrey-testapp-client
