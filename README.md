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
-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:StartFlightRecording:filename=jeffrey-persons-dom-serde-jdk,settings=profile -XX:FlightRecorderOptions:maxchunksize=5m -jar server/target/runner.jar --efficient.mode=false
```

```
// Build the project and create docker images
mvn clean package

// Build container images

cd server && docker build . -t petrbouda/jeffrey-testapp-server -f target/docker/Dockerfile.21-temurin
cd client && docker build . -t petrbouda/jeffrey-testapp-client -f target/docker/Dockerfile.21-temurin

docker run -e EFFICIENT_MODE=false -e DATABASE_IN_MEMORY=true jeffrey-testapp-server

docker run -it --rm --name app --cpus="1" --memory="700m" --memory-swap="700m" --network host -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/asyncprofiler:/tmp/asyncprofiler --security-opt seccomp=unconfined  jfr-test
```
