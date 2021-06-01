# Kubernetes API

* [Java library](https://github.com/kubernetes-client/java)

# Configuration for Deployments

* We need to have access to the appropiate serviceTokenAccount

```
serviceAccountName: sdp (the same as the namespace)
```

```
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: my-deployment
spec:
  template:
    # Below is the podSpec.
    metadata:
      name: ...
    spec:
      serviceAccountName: sdp
```

# Implementation API-GW

* On the api-gw side we will have:

```
minReplicas: 1
maxReplicas: 4
currentReplicas/desiredReplicas: 1-->2

hpa(sdp-user-login) ---> pods(1) ---> svc (sdp-user-login) ---> keys (1*X + (0,5*X))
```

```
minReplicas: 1
maxReplicas: 4
currentReplicas/desiredReplicas: 2-->3

hpa(sdp-user-login) ---> pods(2) ---> svc (sdp-user-login) ---> keys (2*X + (0,5*X))
```

```
minReplicas: 1
maxReplicas: 4
currentReplicas/desiredReplicas: 3-->4

hpa(sdp-user-login) ---> pods(3) ---> svc (sdp-user-login) ---> keys (3*X + (0,5*X))
```

```
minReplicas: 1
maxReplicas: 4
currentReplicas/desiredReplicas: 4

hpa(sdp-user-login) ---> pods(4) ---> svc (sdp-user-login) ---> keys (4*X))
```