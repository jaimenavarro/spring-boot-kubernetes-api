# Kubernetes API

* [Kubernetes Client libraries](https://kubernetes.io/docs/reference/using-api/client-libraries/)
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
