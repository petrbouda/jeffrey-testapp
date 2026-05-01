#!/usr/bin/env bash
#
# Uninstall the full Jeffrey testapp stack from the given namespace and wipe
# the shared hostPath folder so the next install starts from a clean slate.
#
# Usage:
#   helm/uninstall.sh                # jeffrey-testapp namespace (default)
#   helm/uninstall.sh my-namespace   # custom namespace

set -euo pipefail

NAMESPACE="${1:-jeffrey-testapp}"

# Reverse install order.
RELEASES=(jeffrey-testapp-client dom direct jeffrey-server)

for release in "${RELEASES[@]}"; do
    echo "==> [$release] helm uninstall"
    helm uninstall "$release" --namespace "$NAMESPACE" --ignore-not-found
done

# Wipe the shared folder on the cluster node. The hostPath PV uses the
# default reclaimPolicy=Retain for statically-defined PVs, so the directory
# contents at /tmp/jeffrey-data persist on disk after helm removes the PV
# resource — the next install would otherwise inherit a populated
# ${JEFFREY_HOME}/libs/current/. Only applies to the orbstack/hostPath path
# wired up by install.sh; on real clusters the shared volume is dynamically
# provisioned and follows the StorageClass's reclaim policy.
CTX="$(kubectl config current-context 2>/dev/null || echo)"
if [ "$CTX" = "orbstack" ]; then
    SHARED_PATH="/tmp/jeffrey-data"
    echo "==> Wiping shared folder $SHARED_PATH on node ($CTX)"
    kubectl run jeffrey-shared-cleanup \
        --namespace "$NAMESPACE" \
        --rm -i --restart=Never \
        --image=busybox:1.36 \
        --overrides='{"spec":{"containers":[{"name":"jeffrey-shared-cleanup","image":"busybox:1.36","command":["sh","-c","rm -rf /data/* /data/.[!.]* 2>/dev/null; echo cleaned"],"volumeMounts":[{"name":"shared","mountPath":"/data"}]}],"volumes":[{"name":"shared","hostPath":{"path":"'"$SHARED_PATH"'"}}]}}'
fi

echo
echo "Done. Remaining releases in namespace '$NAMESPACE':"
helm list --namespace "$NAMESPACE"
