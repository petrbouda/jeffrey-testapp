#!/usr/bin/env bash
#
# Install or upgrade only the jeffrey-hub release.
#
# Usage:
#   helm/install-hub.sh                           # jeffrey-testapp namespace (default, created if missing)
#   helm/install-hub.sh my-namespace              # custom namespace, will be created
#   helm/install-hub.sh my-ns --dry-run --debug   # extra args forwarded to helm

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"
shift || true   # remaining "$@" is forwarded to helm

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

echo "==> [jeffrey-hub] helm upgrade --install jeffrey-hub"
helm upgrade --install jeffrey-hub "$CHART_DIR/jeffrey-hub" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    "${HUB_EXTRA[@]+"${HUB_EXTRA[@]}"}" \
    "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
