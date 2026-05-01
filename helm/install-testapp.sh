#!/usr/bin/env bash
#
# Install or upgrade the testapp releases (jeffrey-testapp-server in direct + dom
# modes, plus jeffrey-testapp-client). Assumes jeffrey-server is already installed
# — see helm/install-server.sh.
#
# Pod-level ordering against jeffrey-server is enforced by an init container on
# the testapp pods that polls http://jeffrey-server:8080/actuator/health/readiness
# and blocks until 200 — see helm/jeffrey-testapp-{server,client}/templates/deployment.yaml.
# That means this script can run before jeffrey-server is fully ready; the testapp
# pods will simply wait.
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
run jeffrey-testapp-client jeffrey-testapp-client                      "$@"

echo
echo "Done. Releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
