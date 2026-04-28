# Jeffrey - Test App to generate JFR Recordings

A two-module Spring Boot 4 setup used to exercise [Jeffrey](https://www.jeffrey-analyst.cafe)'s profiling and JFR-recording flow. The `server` module is a SQLite-backed REST app; the `client` module drives load against it.

Container images are produced with **JIB** + the `cafe.jeffrey-analyst:jeffrey-jib-maven` extension, which wraps the entrypoint so [Jeffrey](https://github.com/petrbouda/jeffrey) profiling initialises automatically (no Dockerfile, no async-profiler in the image — `jeffrey-cli` is supplied at runtime via a shared volume populated by `jeffrey-server`'s `copy-libs` feature).

## Kubernetes / Helm

```bash
helm/install.sh      # install/upgrade jeffrey-server + testapp-server (direct + dom) + testapp-client into the jeffrey-testapp namespace
helm/uninstall.sh    # tear them all down (reverse order; PV/PVC left to the cluster's reclaim policy)
```

Deployed releases (all in the `jeffrey-testapp` namespace):

- **`jeffrey-server`** — the upstream Jeffrey profiling/observability server. Owns the shared `jeffrey-pvc` and uses `copy-libs` to publish `jeffrey-cli` + agent + `libasyncProfiler` into it for the testapp pods to consume. Exposes HTTP `8080` (REST + actuator) and gRPC `9090` (workspace/recording ingestion).
- **`direct`** (chart `jeffrey-testapp-server`, `mode=direct`) — SQLite-backed REST app running the efficient `PersonService` (`efficient.mode=true`).
- **`dom`** (chart `jeffrey-testapp-server`, `mode=dom`) — same app running the inefficient `PersonService` (`efficient.mode=false`); deployed alongside `direct` so a single workload generates two distinct profiles to compare.
- **`jeffrey-testapp-client`** — single load generator that drives both testapp servers concurrently (each base URL gets its own scheduler).

### Reaching `jeffrey-server` on OrbStack

OrbStack auto-publishes in-cluster Service DNS to the host, so no Ingress controller / `/etc/hosts` edits are needed:

- **HTTP** — `http://jeffrey-server.jeffrey-testapp.svc.cluster.local:8080/` (REST + `/actuator/health`).
- **gRPC** — `jeffrey-server.jeffrey-testapp.svc.cluster.local:9090` (use plaintext h2c — the in-cluster Service is not TLS-fronted; in `jeffrey-local`'s "Connect Remote Workspace" modal, tick **Use plaintext (no TLS)**).
