# Jeffrey - Test App to generate JFR Recordings

#### Setup
- `sudo ln -s /run/user/1000/podman/podman.sock /var/run/docker.sock`
- `docker run -it -v /var/run/docker.sock:/var/run/docker.sock localhost/jeffrey-testapp:latest`
- build the Docker Image: https://github.com/petrbouda/openjdk-x-dbg-asyncprofiler


```
java -agentpath:$ASYNC_HOME/build/lib/libasyncProfiler.so=start,event=cpu,alloc,lock,jfrsync=profile,file=first.jfr -jar server/target/runner.jar
```


```
// Build the project and create docker images
mvn clean package

// Build container images
docker build . -t jeffrey-testapp:21-openjdk-dbg -f server/target/docker/Dockerfile.openjdk-21-dbg
docker build . -t jeffrey-testapp:21-temurin -f server/target/docker/Dockerfile.temurin-21

docker run -it --rm --name app --cpus="1" --memory="700m" --memory-swap="700m" --network host -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/asyncprofiler:/tmp/asyncprofiler --security-opt seccomp=unconfined  jfr-test
docker exec -ti app profiler.sh 60 -e cpu -f /tmp/asyncprofiler/cpu.svg 1
```
