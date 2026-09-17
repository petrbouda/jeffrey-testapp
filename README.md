# Jeffrey - Test App to generate JFR Recordings

A two-module Spring Boot 4 setup used to exercise [Jeffrey](https://www.jeffrey-analyst.cafe)'s profiling and JFR-recording flow. The `server` module is a SQLite-backed REST app; the `client` module drives load against it.

Container images are produced with **JIB** + the `cafe.jeffrey-analyst:jeffrey-jib-maven` extension, which wraps the entrypoint so [Jeffrey](https://github.com/petrbouda/jeffrey) profiling initialises automatically. No Dockerfile and no shell script: the extension also bakes the provisioner and async-profiler into the image under `/opt/jeffrey`, so a pod needs nothing from the cluster to start profiling. The shared volume carries recordings only.

## Kubernetes / Helm

```bash
helm/install.sh      # install/upgrade jeffrey-hub + testapp-server (direct + dom) + testapp-client into the jeffrey-testapp namespace
helm/uninstall.sh    # tear them all down (reverse order; PV/PVC left to the cluster's reclaim policy)
```

Deployed releases (all in the `jeffrey-testapp` namespace):

- **`jeffrey-hub`** — the upstream Jeffrey collector. Owns the shared `jeffrey-pvc` and reconciles the recordings the testapp pods write into it. Exposes HTTP `8080` (REST + actuator) and gRPC `9090` (workspace/recording ingestion).
- **`direct`** (chart `jeffrey-testapp-server`, `mode=direct`) — SQLite-backed REST app running the efficient `PersonService` (`efficient.mode=true`).
- **`dom`** (chart `jeffrey-testapp-server`, `mode=dom`) — same app running the inefficient `PersonService` (`efficient.mode=false`); deployed alongside `direct` so a single workload generates two distinct profiles to compare.
- **`jeffrey-testapp-client`** — single load generator that drives both testapp servers concurrently (each base URL gets its own scheduler).

### Reaching `jeffrey-hub` on OrbStack

OrbStack auto-publishes in-cluster Service DNS to the host, so no Ingress controller / `/etc/hosts` edits are needed:

- **HTTP** — `http://jeffrey-hub.jeffrey-testapp.svc.cluster.local:8080/` (REST + `/actuator/health`).
- **gRPC** — `jeffrey-hub.jeffrey-testapp.svc.cluster.local:9090` (use plaintext h2c — the in-cluster Service is not TLS-fronted; in Jeffrey Microscope's "Connect Remote Workspace" modal, tick **Use plaintext (no TLS)**).
