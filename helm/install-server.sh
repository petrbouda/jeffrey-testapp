#!/usr/bin/env bash
#
# Install or upgrade only the jeffrey-server release.
#
# Usage:
#   helm/install-server.sh                           # jeffrey-testapp namespace (default, created if missing)
#   helm/install-server.sh my-namespace              # custom namespace, will be created
#   helm/install-server.sh my-ns --dry-run --debug   # extra args forwarded to helm

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"
shift || true   # remaining "$@" is forwarded to helm

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

echo "==> [jeffrey-server] helm upgrade --install jeffrey-server"
helm upgrade --install jeffrey-server "$CHART_DIR/jeffrey-server" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    "${SERVER_EXTRA[@]+"${SERVER_EXTRA[@]}"}" \
    "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
