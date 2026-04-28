# Jeffrey - Test App to generate JFR Recordings

A two-module Spring Boot 4 setup used to exercise [Jeffrey](https://www.jeffrey-analyst.cafe)'s profiling and JFR-recording flow. The `server` module is a SQLite-backed REST app; the `client` module drives load against it.

Container images are produced with **JIB** + the `cafe.jeffrey-analyst:jeffrey-jib-maven` extension, which wraps the entrypoint so [Jeffrey](https://github.com/petrbouda/jeffrey) profiling initialises automatically (no Dockerfile, no async-profiler in the image — `jeffrey-cli` is supplied at runtime via a shared volume populated by `jeffrey-server`'s `copy-libs` feature).

## Local run

```bash
mvn -DskipTests package
java -jar server/target/runner.jar    # default SQLite at $TMPDIR/jeffrey-testapp.db
java -jar client/target/runner.jar    # drives load at http://localhost:8080
```

JFR / async-profiler recipes:

```bash
java -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
     -XX:StartFlightRecording:maxage=5m,settings=profile \
     -XX:FlightRecorderOptions:repository=/tmp/jeffrey-testapp \
     -jar server/target/runner.jar

java -agentpath:$ASPROF_HOME/lib/libasyncProfiler.so=start,event=cpu,wall=10ms,alloc,lock,jfrsync=profile,file=first.jfr \
     -jar server/target/runner.jar
```

## Build container images

```bash
# Build to local Docker daemon (no push) — useful for kind / minikube load
mvn -pl server -am -DskipTests compile jib:dockerBuild
mvn -pl client -am -DskipTests compile jib:dockerBuild

# Build and push to Docker Hub
mvn -pl server -am -DskipTests compile jib:build \
    -Djib.to.tags=latest \
    -Djib.to.auth.username=$DOCKERHUB_USERNAME \
    -Djib.to.auth.password=$DOCKERHUB_TOKEN

mvn -pl client -am -DskipTests compile jib:build \
    -Djib.to.tags=latest \
    -Djib.to.auth.username=$DOCKERHUB_USERNAME \
    -Djib.to.auth.password=$DOCKERHUB_TOKEN

# Build to a tar (no daemon required) — for inspection or air-gapped transfer
mvn -pl server -am -DskipTests compile jib:buildTar
```

The resulting images contain:
- `ENTRYPOINT /usr/local/bin/jeffrey-entrypoint` (HOCON-driven init wrapper).
- `CMD java -cp @/app/jib-classpath-file <MainClass>` (JIB-derived).
- ENV default `JEFFREY_BASE_CONFIG=/jeffrey/jeffrey-base.conf` baked into the image. `JEFFREY_HOME` is **not** baked — it must be supplied per-pod (Helm sets it from `values.yaml`); without it the wrapper logs a warning and starts the app without profiling (fail-open).

CI: `.github/workflows/docker-testapp-{server,client}.yml` run `mvn jib:build` on every tag and push to `petrbouda/jeffrey-testapp-{server,client}:<tag>` + `:latest` on Docker Hub.

## Kubernetes / Helm

Three charts live in `helm/`:

- **`helm/jeffrey-server`** — Jeffrey Server itself (formerly `jeffrey-console`). Owns the shared volume; `copy-libs.enabled=true` publishes `jeffrey-cli-<arch>` + agent + libasyncProfiler into `${jeffrey.server.home.dir}/libs/current/`, where the testapp wrappers resolve them. Inspired by the production `azure-sf` deployment: self-profiles via a shell-form command (`/jeffrey-libs/jeffrey-cli-<arch> init … && exec java @/tmp/jvm.args -jar …`), exposes HTTP `8080` and gRPC `9090`, optionally creates the PV+PVC.
- **`helm/jeffrey-testapp-server`** — the SQLite-backed REST app, instrumented with the JIB jeffrey extension. Mounts the same `jeffrey-pvc`. Switches `efficient.mode` via `--set mode=direct|dom`.
- **`helm/jeffrey-testapp-client`** — the load generator, also instrumented and sharing the same PVC.

All three bundle:
- `application.properties` ConfigMap (Spring config), mounted into the container.
- `jeffrey-{base,init}.conf` ConfigMap (HOCON), mounted at the path the CLI expects (`/jeffrey/jeffrey-base.conf` for testapp pods reading via the JIB wrapper; `/jeffrey/jeffrey-init.conf` for the server's self-profile).
- The shared-volume PVC at `/mnt/jeffrey` (override via `sharedVolume.mountPath` — same value must be set on all three charts).

For the common case use the wrapper scripts:

```bash
helm/install.sh                  # install/upgrade all four releases in the jeffrey-testapp namespace
helm/install.sh my-ns --debug    # custom namespace + extra args forwarded to every helm call
helm/uninstall.sh                # tears them all down (reverse order; PV/PVC left to reclaim policy)
```

Install order matters — Jeffrey Server must populate the volume before testapp pods boot, otherwise the JIB wrapper warns and starts the apps without profiling (fail-open):

```bash
# 1. Jeffrey Server first (creates PV+PVC by default; turn that off with
#    --set sharedVolume.create=false if the volume is provisioned externally).
helm upgrade --install jeffrey-server helm/jeffrey-server

# 2. Server, two variants. The client's default base-urls expects Services
#    named `direct-jeffrey-testapp-server` and `dom-jeffrey-testapp-server`.
#    The shortest release names that produce those are `direct` and `dom`
#    (Helm's fullname helper prepends them to the chart name). If you prefer
#    longer release names, the literal `direct-jeffrey-testapp-server` and
#    `dom-jeffrey-testapp-server` also work — the helper detects the chart name
#    as a substring and uses the release name verbatim.
helm upgrade --install direct helm/jeffrey-testapp-server --set mode=direct
helm upgrade --install dom    helm/jeffrey-testapp-server --set mode=dom

# 3. Single client driving BOTH servers concurrently — base-urls splits on
#    `,` and each URL gets its own load scheduler (see ClientApplication.java).
helm upgrade --install jeffrey-testapp-client helm/jeffrey-testapp-client
```

Disable profiling per-pod without rebuilding:

```bash
helm upgrade --install jeffrey-testapp-server helm/jeffrey-testapp-server \
    --set jeffrey.enabled=false
```

Override the HOCON wholesale by editing `helm/jeffrey-testapp-{server,client}/jeffrey-base.conf` before installing, or supply your own ConfigMap and point `JEFFREY_BASE_CONFIG` at its mount path.
