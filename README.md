# Projet M1 — Plateforme PaaS sur AWS EKS

Plateforme pour héberger et exécuter du code (Java, Python, Go) de manière isolée, déployée sur AWS EKS avec CI/CD automatisé et monitoring complet (Prometheus + Grafana).

---

## Architecture

```
GitHub Push → GitHub Actions (CI/CD)
                ↓
         Maven build + Docker image
                ↓
         Docker Hub Registry
                ↓
     ┌─────────────────────────────┐
     │   AWS EKS (Kubernetes)      │
     │   2 workers t3.small        │
     │   2 replicas API            │
     │   Monitoring namespace      │
     └─────────────────────────────┘
              ↓
    Prometheus scrape /actuator/prometheus
              ↓
         Grafana dashboards
```

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.1.2 / Java 17 |
| Build | Maven 3.9 |
| Conteneur | Docker (multi-stage build) |
| Orchestration | Kubernetes 1.29 sur AWS EKS |
| Infrastructure | Terraform (VPC + EKS) |
| CI/CD | GitHub Actions + Docker Hub |
| Monitoring | kube-prometheus-stack (Prometheus + Grafana + AlertManager) |
| Métriques app | Spring Actuator + Micrometer |

---

## 1. Pré-requis

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.6
- [AWS CLI](https://aws.amazon.com/cli/) configuré (`aws configure`)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/) >= 3.x
- Compte Docker Hub

---

## 2. Déploiement de l'infrastructure (Terraform)

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

Terraform crée :
- Un **VPC** avec subnets publics + privés (2 AZ)
- Un cluster **EKS** Kubernetes 1.29
- Un **managed node group** : 2x t3.small (scalable jusqu'à 4)

Récupérer la commande kubectl :
```bash
terraform output kubeconfig_command
# Exemple : aws eks update-kubeconfig --region eu-west-1 --name projetm1-eks
```

---

## 3. Déploiement de l'application

```bash
# Configurer kubectl
aws eks update-kubeconfig --region eu-west-1 --name projetm1-eks

# Déployer namespace monitoring + l'API
kubectl apply -f infra/k8s/

# Vérifier les pods
kubectl get pods
kubectl get svc   # Récupérer l'EXTERNAL-IP du LoadBalancer
```

Tester l'API :
```bash
curl http://<EXTERNAL-IP>/
# → "Bravo ! Ton API Java tourne dans Docker via Kubernetes"

curl http://<EXTERNAL-IP>/actuator/prometheus
# → métriques Prometheus (JVM, HTTP, etc.)
```

---

## 4. Installation du monitoring (Prometheus + Grafana)

```bash
# Ajouter le repo Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Installer kube-prometheus-stack
helm install monitoring prometheus-community/kube-prometheus-stack \
  -f infra/monitoring/prometheus-values.yaml \
  --namespace monitoring --create-namespace

# Attendre que les pods démarrent (2-3 minutes)
kubectl get pods -n monitoring -w

# Récupérer l'IP publique de Grafana
kubectl get svc -n monitoring monitoring-grafana
```

Accéder à Grafana :
- URL : `http://<GRAFANA-EXTERNAL-IP>`
- Login : `admin`
- Mot de passe : `ProjetM1Grafana!`

### Dashboards pré-installés
| Dashboard | ID Grafana | Description |
|-----------|-----------|-------------|
| Kubernetes Cluster | 7249 | CPU, RAM, noeuds |
| JVM Micrometer | 4701 | Métriques Spring Boot (heap, GC, threads) |
| Kubernetes Pods | 6417 | État des déploiements |

---

## 5. CI/CD — GitHub Actions

### Secrets GitHub à configurer

```
Settings → Secrets and variables → Actions → New repository secret
```

| Secret | Valeur |
|--------|--------|
| `DOCKER_USERNAME` | Pseudo Docker Hub |
| `DOCKER_PASSWORD` | Mot de passe Docker Hub |
| `AWS_ACCESS_KEY_ID` | Clé AWS IAM |
| `AWS_SECRET_ACCESS_KEY` | Secret AWS IAM |
| `AWS_REGION` | ex: `eu-west-1` |
| `EKS_CLUSTER_NAME` | `projetm1-eks` |

### Pipeline automatique

Chaque `git push` sur `main` déclenche :
1. Compilation Maven + build Docker image
2. Push image sur Docker Hub (tagué avec le SHA du commit)
3. Connexion au cluster EKS
4. Rolling update automatique du déploiement

---

## 6. Structure du projet

```
.
├── src/main/java/com/example/App.java          # API Spring Boot
├── src/main/resources/application.properties   # Config Actuator/Prometheus
├── pom.xml                                      # Dépendances Maven
├── dockerfile                                   # Multi-stage build
├── infra/
│   ├── terraform/
│   │   ├── versions.tf                          # Providers Terraform
│   │   ├── variables.tf                         # Variables (région, type instance...)
│   │   ├── main.tf                              # VPC + EKS
│   │   └── outputs.tf                           # Outputs (endpoint, commande kubectl)
│   ├── k8s/
│   │   ├── namespace.yaml                       # Namespace monitoring
│   │   └── deployment.yaml                      # Déploiement K8s + Service LoadBalancer
│   └── monitoring/
│       └── prometheus-values.yaml               # Helm values kube-prometheus-stack
└── .github/workflows/cicd.yaml                  # Pipeline CI/CD
```

---

## 7. Équipe

| Membre | Rôle |
|--------|------|
| DENILSSON | Infrastructure AWS EKS (Terraform, K8s) |
| HAFSA | CI/CD (GitHub Actions, Docker Hub) |
| GWENN | Monitoring (Prometheus, Grafana) |

---

## 8. Notes sur les coûts AWS

> Estimation pour un projet de courte durée (à supprimer après la soutenance) :
> - EKS cluster control plane : ~0.10 $/h
> - 2x t3.small workers : ~0.04 $/h
> - NAT Gateway : ~0.05 $/h
> - Load Balancers (x2) : ~0.02 $/h
> - **Total estimé : ~0.21 $/h (~5 $/jour)**

Pour supprimer toute l'infra :
```bash
helm uninstall monitoring -n monitoring
kubectl delete -f infra/k8s/
cd infra/terraform && terraform destroy
```
