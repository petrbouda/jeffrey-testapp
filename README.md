# Jeffrey - Test App to generate JFR Recordings

A two-module Spring Boot 4 setup used to exercise [Jeffrey](https://www.jeffrey-analyst.cafe)'s profiling and JFR-recording flow, and to produce the example recordings Jeffrey ships with. The `server` module is a SQLite-backed REST app; the `client` module drives load against it.

Container images are produced with **JIB** + the `cafe.jeffrey-analyst:jeffrey-jib-maven-jar` extension, which wraps the entrypoint so [Jeffrey](https://github.com/petrbouda/jeffrey) profiling initialises automatically. No Dockerfile and no shell script: the extension also bakes the provisioner and async-profiler into the image under `/opt/jeffrey`, so a pod needs nothing from the cluster to start profiling. The shared volume carries recordings only.

## Versions

| What | Version | Where |
|---|---|---|
| `jeffrey-events`, `jeffrey-tracing-*` | 0.27.0 | `jeffrey.version` in `pom.xml` (one version, they release together) |
| `jeffrey-jib-maven-jar` | 0.7.0 (provisioner from Jeffrey v0.14.8, async-profiler 4.1) | `jeffrey-jib.version` in `pom.xml` |
| Spring Boot | 4.1.2 | `springboot.version` |
| Java | 25 | `eclipse-temurin:25-jre` base image |

## What the recordings contain

Both apps depend on `jeffrey-events`, `jeffrey-tracing-spring-boot-starter` and `jeffrey-tracing-scoped-value` (the `ScopedValue` span storage, which takes over from the starter's `ThreadLocal` one on Java 25).

**Server** (`jeffrey-testapp-server`):

- **Traces** — the starter's servlet filter records every request as `jeffrey.HttpServerExchange` and opens it as the root span, named by the matched route (`GET /persons/{count}`). Query and path parameters are captured, and every request span carries a `testapp.mode` attribute (`direct`, `dom`, `n-plus-one`, `lock` or `exceptions`), so the modes are a facet in Traces → Attributes.
- **Hand-written spans** (`Tracer`) between the request and its SQL:
  - `person.pick-id` / `person.pick-ids` — in `dom` mode they hold an extra `count_query` statement, in `direct` mode none; same name, so the two line up.
  - `person.redact-opinions` — the regex over the loaded rows.
  - `person.to-json-tree` — the `dom` mode's detour through `JsonNode`, so its garbage is attributed to a span.
- **JDBC** — `DatabaseClient` emits a labelled `jeffrey.Jdbc*` event per statement (`find_by_id`, `insert_person`, …, grouped under `person-repository`) with parameters and row counts, nested under the span in progress. The starter's `DataSource` wrapper is turned off (`jeffrey.tracing.jdbc-enabled=false`) so nothing is recorded twice.
- **Connection pool** — the starter gives the HikariCP pool its JFR metrics tracker: acquired/borrowed/created/timeout events and the periodic `jeffrey.JdbcPoolStatistics`.
- **Backup job** — every `backup.interval` (default `5m`) the whole table is dumped, deleted and re-inserted row by row under the write lock: one `person.backup-and-reload` trace with ~1,000 `backup_load` inserts (an N+1 shape to find), every request blocked on the lock meanwhile, and a `jeffrey.Notification` (`PERSON_BACKUP_RELOADED`) recorded inside that span.
- `--native-memory-leak.mode=true` leaks every other native allocation and records a `NATIVE_MEMORY_LEAK_MODE_ENABLED` notification at startup.

**Client** (`jeffrey-testapp-client`): every scheduled load task is the root span of its own trace (`load.get-person`, `load.remove-person`, …), with each outbound call recorded as a `jeffrey.HttpClientExchange` leaf by the starter's `JfrClientHttpRequestInterceptor` (numeric path segments collapsed to `{n}`). `GET /persons` returns 404 for ids the client has already removed, so some traces are `ERROR`.

## Running locally

```bash
mvn package -DskipTests
```

```bash
java -XX:StartFlightRecording=filename=server.jfr,settings=profile -jar server/target/runner.jar --testapp.mode=dom --backup.interval=1m   # or direct, n-plus-one, lock, exceptions
```

```bash
java -XX:StartFlightRecording=filename=client.jfr,settings=profile -jar client/target/runner.jar
```

Check the spans before using a recording: `jfr print --events "jeffrey.*" server.jfr`.

## Kubernetes / Helm

```bash
helm/install.sh      # install/upgrade jeffrey-volume + testapp-server (one release per mode) + testapp-client into the jeffrey-testapp namespace
helm/uninstall.sh    # tear them all down (reverse order; PV/PVC left to the cluster's reclaim policy)
```

Deployed releases (all in the `jeffrey-testapp` namespace):

- **`jeffrey-volume`** — the shared recording volume: the `jeffrey-pvc` claim the testapp pods write their recordings into, plus a hostPath PV at `/tmp/jeffrey-data` on OrbStack, where there is no RWX provisioner.
- **`direct`** (chart `jeffrey-testapp-server`, `mode=direct`) — SQLite-backed REST app running the efficient `PersonService`: the baseline.
- **`dom`** (`mode=dom`) — the inefficient `PersonService`: a `COUNT` before every random pick (inside `person.pick-id(s)`) and every response built as a JSON tree (`person.to-json-tree`).
- **`n-plus-one`** (`mode=n-plus-one`) — the baseline, but `GET /persons/{count}` loads its persons one `find_by_id` statement each inside `person.load-each` (`NPlusOnePersonService`), where the baseline runs one `find_all_by_ids`.
- **`lock`** (`mode=lock`) — the baseline, but every lookup runs in a `synchronized` method of the one `LockPersonService`, so its monitor is held across the database call and concurrent requests queue on it (`jdk.JavaMonitorEnter` on `LockPersonService`).
- **`exceptions`** (`mode=exceptions`) — the baseline, but every served person's phone is normalized by trying `Long.parseLong` and catching the `NumberFormatException` (`ExceptionsPersonService`, `person.normalize-phones`): the stored phones are formatted, so every person throws one.
- **`jeffrey-testapp-client`** — single load generator that drives every testapp server concurrently (each base URL gets its own scheduler, so each gets the same load).

The mode is `testapp.mode` (the chart renders it from `mode`); without it, `efficient.mode` picks `direct` (true) or `dom` (false) as before. Every mode but `dom` is `direct` with one problem planted, so a recording of a mode set against a `direct` recording taken under the same load shows that problem alone.

Each pod's provisioner config is `helm/<chart>/jeffrey-base.conf` (`tracing`, perf counters, heap dump on crash, JVM options). The project name is set by each Deployment as `JEFFREY_PROJECT_NAME` (`<mode>-jeffrey-testapp-server` — `direct-…`, `dom-…`, `n-plus-one-…`, `lock-…`, `exceptions-…` — and `jeffrey-testapp-client`), because jeffrey-jib bakes the artifactId into the image under that variable and an environment variable wins over the config file.

### Reading the recordings

Microscope reads the volume itself: point its connector at the directory the shared volume is in (with the OrbStack setup, the hostPath PV at `/tmp/jeffrey-data` on the cluster node), or at a copy of it:

```properties
jeffrey.microscope.connector.local.path=/tmp/jeffrey-data
jeffrey.microscope.connector.local.read-only=true
```
