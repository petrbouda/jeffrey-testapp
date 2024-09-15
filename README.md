# Jeffrey - Test App to generate JFR Recordings

```
java -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:StartFlightRecording:maxage=5m,settings=profile -XX:FlightRecorderOptions:repository=/tmp/jeffrey-testapp -jar server/target/runner.jar```

```
# sysctl kernel.perf_event_paranoid=1
# sysctl kernel.kptr_restrict=0

java -agentpath:$ASPROF_HOME/lib/libasyncProfiler.so=start,event=cpu,alloc,lock,jfrsync=profile,file=first.jfr -jar server/target/runner.jar
```

```
java -XX:-UseTLAB -XX:StartFlightRecording:filename=/tmp/allocation.jfr,dumponexit=true,settings=none,+jdk.ObjectAllocationOutsideTLAB#enabled=true,+jdk.ActiveRecording#enabled=true -jar server/target/runner.jar
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
