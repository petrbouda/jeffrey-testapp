# Install Jeffrey Test App using Helm

#### Manually adding PV/PVC
```
kubectl apply -f ./jeffrey-console/templates/persistent-volume.yaml

kubectl delete -f ./jeffrey-console/templates/persistent-volume-claim.yaml && \
kubectl delete -f ./jeffrey-console/templates/persistent-volume.yaml
```

#### Start Minikube and deploy Applications using Helm

```
minikube start --cpus 6 --memory 16g
minikubed dashboard
```

```
helm upgrade --install jeffrey-console ./jeffrey-console && \
helm upgrade --install jeffrey-testapp-client ./jeffrey-testapp-client  && \
helm upgrade --install jeffrey-testapp-dom-server ./jeffrey-testapp-dom-server  && \
helm upgrade --install jeffrey-testapp-direct-server  ./jeffrey-testapp-direct-server
```

```
helm uninstall jeffrey-console && \
helm uninstall jeffrey-testapp-client && \
helm uninstall jeffrey-testapp-dom-server && \
helm uninstall jeffrey-testapp-direct-server
```
