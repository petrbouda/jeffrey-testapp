#!/usr/bin/env bash
#
# Install or upgrade the full Jeffrey testapp stack (jeffrey-hub +
# jeffrey-testapp-server x2 modes + jeffrey-testapp-client).
#
# Usage:
#   helm/install.sh                           # jeffrey-testapp namespace (default, created if missing)
#   helm/install.sh my-namespace              # custom namespace, will be created
#   helm/install.sh my-ns --dry-run --debug   # extra args forwarded to every helm call
#
# jeffrey-hub creates the shared PVC that the testapp pods write their recordings into.
# Install order does not matter and no pod waits for another: every application image
# carries its own provisioner and async-profiler, baked in at build time by jeffrey-jib,
# so a testapp pod scheduled before jeffrey-hub profiles from its first second and the
# hub picks the recordings up whenever it arrives.

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"
shift || true   # remaining "$@" is forwarded to every helm call

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Single-node local clusters (orbstack on macOS) have no RWX provisioner, so the
# default `nfs` StorageClass leaves the PVC unbound. Fall back to a static
# hostPath PV at /tmp/jeffrey-data — `storageClassName=""` is required so the
# default-storage-class admission controller doesn't auto-fill it and break the
# static binding (see helm/jeffrey-hub/templates/persistent-volume-claim.yaml).
HUB_EXTRA=()
CTX="$(kubectl config current-context 2>/dev/null || echo)"
if [ "$CTX" = "orbstack" ]; then
    echo "==> orbstack context detected — using hostPath PV for jeffrey-hub"
    HUB_EXTRA+=(--set sharedVolume.storageClassName="" --set sharedVolume.hostPath.create=true)
fi

run() {
    local release="$1" chart="$2"; shift 2
    echo "==> [$release] helm upgrade --install $chart"
    helm upgrade --install "$release" "$CHART_DIR/$chart" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        "$@"
}

run jeffrey-hub            jeffrey-hub             "${HUB_EXTRA[@]+"${HUB_EXTRA[@]}"}"  "$@"
run direct                 jeffrey-testapp-server  --set mode=direct   "$@"
run dom                    jeffrey-testapp-server  --set mode=dom      "$@"
run jeffrey-testapp-client jeffrey-testapp-client                      "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
