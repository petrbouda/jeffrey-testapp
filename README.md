# CGROUPS with JAVA

#### Setup
- `sudo ln -s /run/user/1000/podman/podman.sock /var/run/docker.sock`
- `docker run -it -v /var/run/docker.sock:/var/run/docker.sock localhost/jeffrey-testapp:latest`
- build the Docker Image: https://github.com/petrbouda/openjdk-x-dbg-asyncprofiler

```
// Build the project and create docker images
mvn clean package

docker run -it --rm --name app --cpus="1" --memory="700m" --memory-swap="700m" --network host -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/asyncprofiler:/tmp/asyncprofiler --security-opt seccomp=unconfined  jfr-test
docker exec -ti app profiler.sh 60 -e cpu -f /tmp/asyncprofiler/cpu.svg 1
```

#### Build a docker image with async-profiler

```
docker build -t openjdk-15-asyncprofiler:1.8.1 ./async-profiler-image
```
Copy libasyncProfiler.so to be on the same place as in the docker container
```
mkdir /tmp/asyncprofiler && cp ./async-profiler-image/libasyncProfiler.so /tmp/asyncprofiler/
```
