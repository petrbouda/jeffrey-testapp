#!/usr/bin/env bash
#
# Install or upgrade the full Jeffrey testapp stack (jeffrey-server +
# jeffrey-testapp-server x2 modes + jeffrey-testapp-client).
#
# Usage:
#   helm/install.sh                           # jeffrey-testapp namespace (default, created if missing)
#   helm/install.sh my-namespace              # custom namespace, will be created
#   helm/install.sh my-ns --dry-run --debug   # extra args forwarded to every helm call
#
# jeffrey-server creates the shared PVC and populates ${JEFFREY_HOME}/libs/current/
# via copy-libs (which runs from inside the app after JVM start). Pod-level ordering
# is enforced by an init container on the testapp pods that polls
# http://jeffrey-server:8080/actuator/health/readiness and blocks until 200 — see
# helm/jeffrey-testapp-{server,client}/templates/deployment.yaml. The init script
# itself can therefore install the releases in any order without operator-side waits.

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"
shift || true   # remaining "$@" is forwarded to every helm call

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Single-node local clusters (orbstack on macOS) have no RWX provisioner, so the
# default `nfs` StorageClass leaves the PVC unbound. Fall back to a static
# hostPath PV at /tmp/jeffrey-data — `storageClassName=""` is required so the
# default-storage-class admission controller doesn't auto-fill it and break the
# static binding (see helm/jeffrey-server/templates/persistent-volume-claim.yaml).
SERVER_EXTRA=()
CTX="$(kubectl config current-context 2>/dev/null || echo)"
if [ "$CTX" = "orbstack" ]; then
    echo "==> orbstack context detected — using hostPath PV for jeffrey-server"
    SERVER_EXTRA+=(--set sharedVolume.storageClassName="" --set sharedVolume.hostPath.create=true)
fi

run() {
    local release="$1" chart="$2"; shift 2
    echo "==> [$release] helm upgrade --install $chart"
    helm upgrade --install "$release" "$CHART_DIR/$chart" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        "$@"
}

run jeffrey-server         jeffrey-server          "${SERVER_EXTRA[@]+"${SERVER_EXTRA[@]}"}"  "$@"
run direct                 jeffrey-testapp-server  --set mode=direct   "$@"
run dom                    jeffrey-testapp-server  --set mode=dom      "$@"
run jeffrey-testapp-client jeffrey-testapp-client                      "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
