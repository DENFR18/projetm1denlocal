# Rapport Technique — Projet M1 PaaS sur AWS EKS

**Date :** 14 mars 2026
**Équipe :** DENILSSON, HAFSA, GWENN
**Repo :** https://github.com/DENFR18/projetm1denlocal
**Branche principale :** `main`

---

## 1. Objectif du projet

Concevoir et déployer une **plateforme PaaS (Platform as a Service)** permettant d'héberger et d'exécuter du code (Python, Node.js) dans des conteneurs Kubernetes isolés, avec :
- Infrastructure provisionnée automatiquement via Terraform sur AWS
- Pipeline CI/CD complet avec GitHub Actions
- Monitoring en temps réel (Prometheus + Grafana)
- Auto-scaling selon la charge

---

## 2. Architecture déployée

```
Développeur
    │
    ▼ git push main
GitHub Actions (CI/CD — 4 jobs)
    │
    ├─ 1. Build & Test (Maven + Java 17)
    ├─ 2. Docker Build & Push → Docker Hub (den95/projetm1)
    ├─ 3. Scan Sécurité (Trivy — vulnérabilités CRITICAL/HIGH)
    └─ 4. Deploy → AWS EKS
              │
              ▼
    ┌─────────────────────────────────────────────┐
    │            AWS EKS (eu-west-1)              │
    │                                             │
    │  VPC (2 AZ) ── Subnets publics + privés    │
    │       │                                     │
    │  EKS Cluster Kubernetes 1.29                │
    │       ├── Node 1 (t3.small)                 │
    │       └── Node 2 (t3.small)                 │
    │                                             │
    │  Namespace: default                         │
    │  ├── Deployment: api-backend (2 replicas)   │
    │  ├── Service: LoadBalancer (AWS ELB)        │
    │  ├── HPA: autoscaling (2 → 6 pods)         │
    │  ├── ServiceAccount + RBAC                  │
    │  └── Secret: dockerhub-credentials          │
    │                                             │
    │  Namespace: monitoring                      │
    │  ├── Prometheus (scraping toutes les 15s)   │
    │  ├── Grafana (dashboards K8s + JVM)         │
    │  └── AlertManager                           │
    └─────────────────────────────────────────────┘
```

---

## 3. Ce qui a été réalisé — Checklist complète

### 3.1 Infrastructure (Terraform)

| Élément | Statut | Détails |
|---------|--------|---------|
| Provider AWS configuré | ✅ | `infra/terraform/versions.tf` |
| VPC dédié | ✅ | 2 AZ, subnets publics + privés, NAT Gateway |
| Cluster EKS | ✅ | Kubernetes 1.29, eu-west-1 |
| Managed Node Group | ✅ | 2x t3.small (scalable jusqu'à 4) |
| IAM Roles EKS | ✅ | Attachés automatiquement par le module Terraform |
| Outputs Terraform | ✅ | endpoint, kubeconfig_command |

**Commandes :**
```bash
cd infra/terraform
terraform init && terraform apply
# → 56 ressources créées
```

### 3.2 Application Spring Boot

| Élément | Statut | Détails |
|---------|--------|---------|
| API REST Spring Boot 3.1.2 / Java 17 | ✅ | `src/main/java/com/example/App.java` |
| Interface web (dark mode) | ✅ | `src/main/resources/static/index.html` |
| Éditeur Python + Node.js | ✅ | Exécution réelle via ProcessBuilder + timeout 10s |
| Métriques Prometheus | ✅ | `/actuator/prometheus` via Micrometer |
| Multi-stage Docker | ✅ | Maven → JRE Alpine + python3 + nodejs |

**Endpoints disponibles :**
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/` | Interface web PaaS |
| GET | `/api/hello?name=X` | Hello world |
| GET | `/api/status` | Statut + version |
| POST | `/api/deployments/deploy` | Exécuter Python ou Node.js |

**URL de production :**
```
http://ada47e3e855844b5c8d16141144c35b2-78806808.eu-west-1.elb.amazonaws.com
```

### 3.3 Kubernetes — Manifestes

| Ressource | Statut | Fichier |
|-----------|--------|---------|
| Deployment (2 replicas) | ✅ | `infra/k8s/deployment.yaml` |
| Service LoadBalancer | ✅ | `infra/k8s/deployment.yaml` |
| Namespace monitoring | ✅ | `infra/k8s/namespace.yaml` |
| HPA (autoscaling v2) | ✅ | `infra/k8s/hpa.yaml` |
| ServiceAccount RBAC | ✅ | `infra/k8s/rbac.yaml` |
| Role + RoleBinding | ✅ | `infra/k8s/rbac.yaml` |
| Secret Docker Hub | ✅ | `infra/k8s/secret-dockerhub.yaml` |

**État du cluster au 14/03/2026 :**
```
NAME                                          READY   STATUS
pod/api-backend-deployment-xxx                1/1     Running
pod/api-backend-deployment-yyy                1/1     Running

HPA: CPU 3%/60%  |  RAM 42%/75%  |  Replicas: 2/6
```

**Détails RBAC :**
- `ServiceAccount` : `api-backend-sa`
- `Role` : lecture seule sur pods, services, deployments
- `RoleBinding` : lié au ServiceAccount dans namespace `default`

### 3.4 CI/CD — GitHub Actions

| Job | Statut | Description |
|-----|--------|-------------|
| Build & Test (Maven) | ✅ | Compile + package JAR |
| Docker Build & Push | ✅ | Tag par SHA commit + latest |
| Scan Trivy | ✅ | CRITICAL/HIGH, rapport SARIF en artifact |
| Deploy EKS | ✅ | Rolling update + HPA + RBAC automatiques |

**Déclencheur :** `git push` sur `main`
**Secrets configurés :** `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `EKS_CLUSTER_NAME`

### 3.5 Monitoring

| Élément | Statut | Détails |
|---------|--------|---------|
| kube-prometheus-stack | ✅ | Helm, namespace monitoring |
| Prometheus scraping | ✅ | Annotations sur les pods, toutes les 15s |
| Grafana | ✅ | Accessible via LoadBalancer |
| Dashboard Kubernetes Cluster | ✅ | CPU, RAM, nœuds |
| Dashboard JVM Micrometer | ✅ | Heap, GC, threads Spring Boot |
| metrics-server | ✅ | Requis pour HPA |

**Accès Grafana :**
```
URL : http://a70be0f3a68864a3f98131e8bfe1fa32-2005962980.eu-west-1.elb.amazonaws.com
Login : admin / ProjetM1Grafana!
```

---

## 4. Conformité avec les exigences du cahier des charges

| Exigence | Couvert | Comment |
|----------|---------|---------|
| Plateforme PaaS opérationnelle | ✅ | API + interface web en prod sur AWS |
| Conteneurs Docker | ✅ | Image multi-stage, publiée sur Docker Hub |
| Orchestration Kubernetes | ✅ | EKS 1.29, 2 workers, 2 replicas |
| Infrastructure as Code (Terraform) | ✅ | VPC + EKS modulaires |
| Pipeline CI/CD | ✅ | 4 jobs GitHub Actions |
| Monitoring (Prometheus + Grafana) | ✅ | kube-prometheus-stack complet |
| Sécurité (scan vulnérabilités) | ✅ | Trivy CRITICAL/HIGH |
| Auto-scaling | ✅ | HPA v2 CPU + RAM |
| RBAC Kubernetes | ✅ | ServiceAccount + Role + RoleBinding |
| Secrets Kubernetes | ✅ | Secret Docker Hub (imagePullSecret) |
| Green IT (limites ressources) | ✅ | requests/limits sur le Deployment |
| Isolation d'exécution | ✅ | ProcessBuilder avec timeout 10s |

---

## 5. Structure du dépôt Git

```
projetm1denlocal/
├── src/
│   └── main/
│       ├── java/com/example/App.java          # API + exécution code
│       └── resources/
│           ├── static/index.html              # Interface web
│           └── application.properties         # Config Actuator
├── dockerfile                                  # Multi-stage (JRE + Python + Node)
├── pom.xml                                     # Spring Boot + Micrometer
├── infra/
│   ├── terraform/                              # IaC — VPC + EKS
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── versions.tf
│   │   └── outputs.tf
│   ├── k8s/                                   # Manifestes Kubernetes
│   │   ├── deployment.yaml                    # Deployment + Service
│   │   ├── hpa.yaml                           # HorizontalPodAutoscaler
│   │   ├── rbac.yaml                          # ServiceAccount + Role + RoleBinding
│   │   ├── secret-dockerhub.yaml              # Secret registry
│   │   └── namespace.yaml                     # Namespace monitoring
│   └── monitoring/
│       └── prometheus-values.yaml             # Helm values Grafana + Prometheus
└── .github/
    └── workflows/
        └── ci-cd.yml                          # Pipeline CI/CD 4 jobs
```

---

## 6. Ce qu'il reste à faire (livrables)

| Livrable | Responsable | Deadline |
|----------|-------------|---------|
| Screenshots Grafana (dashboards JVM + K8s) | GWENN | Avant arrêt AWS |
| Vidéo MVP 15-20 min (démo live) | Groupe | Soutenance |
| Document technique PDF (groupe) | Groupe | Soutenance |
| Document individuel PDF (contribution) | Chacun | Soutenance |

> **Important :** Le cluster AWS tourne en continu (~5 $/jour). Faire les screenshots Grafana rapidement. Arrêter l'infra avec `terraform destroy` après la soutenance.

---

## 7. Commandes utiles — Rappel

```bash
# Accéder au cluster
aws eks update-kubeconfig --region eu-west-1 --name projetm1-eks

# État des pods
kubectl get pods,svc,hpa

# Logs de l'API
kubectl logs -l app=api-backend --tail=50

# Supprimer toute l'infra (après soutenance)
helm uninstall monitoring -n monitoring
kubectl delete -f infra/k8s/
cd infra/terraform && terraform destroy
```

---

## 8. Points forts à mettre en avant à la soutenance

1. **Infrastructure 100% automatisée** — un `terraform apply` suffit à recréer l'environnement complet
2. **Zero-downtime deployment** — rolling update Kubernetes sans interruption de service
3. **Pipeline CI/CD sécurisé** — scan Trivy intégré bloque en cas de vulnérabilité critique
4. **Observabilité complète** — métriques JVM + Kubernetes visibles en temps réel dans Grafana
5. **RBAC** — principe du moindre privilège appliqué (lecture seule sur les ressources K8s)
6. **Green IT** — limites CPU/RAM explicites pour éviter la sur-consommation
7. **Exécution de code isolée** — Python et Node.js dans le conteneur avec timeout sécurisé
