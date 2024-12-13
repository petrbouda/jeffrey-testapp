# Install Jeffrey Test App using Helm

```
helm upgrade --install jeffrey-console ./jeffrey-console
helm upgrade --install jeffrey-testapp-client ./jeffrey-testapp-client
helm upgrade --install jeffrey-testapp-dom-server ./jeffrey-testapp-dom-server
helm upgrade --install jeffrey-testapp-direct-server  ./jeffrey-testapp-direct-server
```

```
helm uninstall jeffrey-console
helm uninstall jeffrey-testapp-client
helm uninstall jeffrey-testapp-dom-server
helm uninstall jeffrey-testapp-direct-server
```
