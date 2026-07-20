SmartPayHub Helm Chart

This chart deploys all SmartPayHub microservices as defined in `values.yaml`.

Usage:

Install:

helm install smartpayhub ./helm/smartpayhub -n smartpayhub --create-namespace

Upgrade:

helm upgrade smartpayhub ./helm/smartpayhub -n smartpayhub

Custom values example:

helm install smartpayhub ./helm/smartpayhub -n smartpayhub --set services[0].image=smartpayhub/sph-api-gateway:v1.2.3 --set secrets.dbPasswordSecret=$(kubectl create secret generic mysecret --from-literal=db.password=supersecret --dry-run=client -o yaml)

