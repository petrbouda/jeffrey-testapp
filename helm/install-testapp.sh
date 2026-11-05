#!/usr/bin/env bash
#
# Install or upgrade the testapp releases (jeffrey-testapp-server in its five modes —
# direct, dom, n-plus-one, lock, exceptions — plus jeffrey-testapp-client). Assumes the shared volume (jeffrey-volume chart,
# installed by helm/install.sh) already exists: the pods bind to its jeffrey-pvc claim.
# Each image carries its own provisioner and async-profiler, so a testapp pod profiles
# from its first second.
#
# Usage:
#   helm/install-testapp.sh                           # jeffrey-testapp namespace (default, created if missing)
#   helm/install-testapp.sh my-namespace              # custom namespace, will be created
#   helm/install-testapp.sh my-ns --dry-run --debug   # extra args forwarded to every helm call

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"
shift || true   # remaining "$@" is forwarded to every helm call

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

run() {
    local release="$1" chart="$2"; shift 2
    echo "==> [$release] helm upgrade --install $chart"
    helm upgrade --install "$release" "$CHART_DIR/$chart" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        "$@"
}

run direct                 jeffrey-testapp-server  --set mode=direct   "$@"
run dom                    jeffrey-testapp-server  --set mode=dom      "$@"
run n-plus-one             jeffrey-testapp-server  --set mode=n-plus-one "$@"
run lock                   jeffrey-testapp-server  --set mode=lock     "$@"
run exceptions             jeffrey-testapp-server  --set mode=exceptions "$@"
run jeffrey-testapp-client jeffrey-testapp-client                      "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
