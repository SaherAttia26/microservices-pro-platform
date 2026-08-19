# Product service Kubernetes setup

The Deployment and Canary Deployment use Spring Boot Actuator liveness and readiness probes.
They both require a `product-secrets` Secret containing `DB_PASSWORD` before ArgoCD can sync
them successfully. The repository deliberately does not contain database credentials.

Create the Secret in the target cluster, using a value from your local environment or secret manager:

```powershell
kubectl create secret generic product-secrets `
  --namespace ecommerce `
  --from-literal=DB_PASSWORD='<database-password>'
```

For a first deploy, replace the `sha-REPLACE_ME` image tags in both deployment manifests with
an immutable image tag published by the Session 13 workflow. Then apply the ArgoCD Application
manifest from `k8s/argocd/product-service-app.yaml`.
