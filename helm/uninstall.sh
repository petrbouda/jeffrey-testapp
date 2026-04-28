#!/usr/bin/env bash
#
# Uninstall the full Jeffrey testapp stack from the given namespace.
#
# Usage:
#   helm/uninstall.sh                # jeffrey-testapp namespace (default)
#   helm/uninstall.sh my-namespace   # custom namespace
#
# Releases are removed in reverse install order (client first, jeffrey-server
# last). The shared PVC and any data on the PV are left to the cluster's
# reclaim policy — clean those up manually with `kubectl delete pvc/pv` if
# you want a fully fresh slate.

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"

# Reverse install order.
RELEASES=(jeffrey-testapp-client dom direct jeffrey-server)

for release in "${RELEASES[@]}"; do
    echo "==> [$release] helm uninstall"
    helm uninstall "$release" --namespace "$NAMESPACE" --ignore-not-found
done

echo
echo "Done. Remaining releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"

echo
echo "Note: PVC 'jeffrey-pvc' and PV may still exist depending on reclaim policy."
echo "      Inspect with:  kubectl -n $NAMESPACE get pvc,pv | grep jeffrey"
