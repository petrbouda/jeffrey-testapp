docker build . --no-cache -t petrbouda/temurin-asprof:24 -f Dockerfile.temurin-asprof
docker tag petrbouda/temurin-asprof:24 petrbouda/temurin-asprof:latest
docker push petrbouda/temurin-asprof:24
docker push petrbouda/temurin-asprof:latest
