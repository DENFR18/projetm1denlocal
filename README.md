🚀 PROJET M1 : PLATEFORME DE DISTRIBUTION DE CODE (Dossier Technique Complet)
Rappel du but : On construit une plateforme pour héberger et exécuter du code (Java, Python, Go) de manière isolée. Pour ça, on a monté une infrastructure K8s avec une chaîne CI/CD et du Monitoring.

🏗️ 1. DENILSSON : L'Infrastructure et le Socle K8s
Mon rôle : Créer les serveurs et exposer notre API sur le web.

💻 Phase 1 : Le crash-test en local (À raconter au prof)
J'ai commencé par tout monter sur mon PC Windows pour tester l'API.

Les commandes : J'ai monté des VM avec vagrant up et j'y ai mis K3s (Kubernetes léger). Ensuite, j'ai dû bypasser la sécurité Windows pour lancer mes scripts :

PowerShell
Set-ExecutionPolicy Bypass -Scope Process
Le déploiement local : J'ai utilisé ce fichier deployment.yaml basique pour lancer notre API Java :

YAML
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-backend-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-backend
  template:
    metadata:
      labels:
        app: api-backend
    spec:
      containers:
      - name: api-backend
        image: 953p0/projetm1:latest
        ports:
        - containerPort: 8080
Le problème (L'argument en or) : Mon PC n'a que 16 Go de RAM. Quand je tapais kubectl get pods -w, mes pods restaient bloqués en ContainerCreating pendant des plombes, et finissaient par crasher en OOMKilled (Out of Memory) quand on essayait d'ajouter les sondes de métriques. L'infra locale ne tenait pas la route.

☁️ Phase 2 : Le passage sur Azure (AKS)
Pour que vous puissiez bosser proprement, j'ai tout migré sur le Cloud Microsoft Azure.

Les commandes de création (Ce que j'ai tapé pour vous créer le serveur) :

PowerShell
# 1. Je me connecte
az login

# 2. Je crée le groupe de ressources
az group create --name ProjetM1-RG --location westeurope

# 3. Je crée le vrai cluster K8s
az aks create --resource-group ProjetM1-RG --name ProjetM1Cluster --node-count 2

# 4. Je relie mon terminal au cluster
az aks get-credentials --resource-group ProjetM1-RG --name ProjetM1Cluster
L'astuce finale : J'ai modifié le fichier service.yaml de l'API pour changer le type: NodePort en type: LoadBalancer. Grâce à ça, Azure nous a filé une IP Publique. Notre API est en ligne, le socle est prêt.

⚙️ 2. HAFSA : La CI/CD (L'Usine Logicielle)
Ton rôle : Automatiser le déploiement de l'API. Si on modifie le code Java, ton robot doit s'occuper de tout envoyer sur Azure.

💻 Phase 1 : Comprendre Docker
Notre API doit être mise dans une "boîte" étanche.

Le code (Ton Dockerfile) : Voici à quoi ressemble la recette pour empaqueter notre app Java. C'est ce fichier qui doit être à la racine de notre code :

Dockerfile
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/api-backend.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
Tes commandes manuelles pour tester :

PowerShell
docker build -t 953p0/projetm1-api:latest .
docker run -p 8080:8080 953p0/projetm1-api:latest
☁️ Phase 2 : Le Workflow GitHub Actions (Ton vrai boulot)
Tu dois automatiser les commandes Docker du dessus.

Sur GitHub : Va dans Settings > Secrets and variables > Actions et ajoute DOCKER_USERNAME (notre pseudo) et DOCKER_PASSWORD.

Le code du robot : Dans notre projet, crée ce fichier exact : .github/workflows/cicd.yaml et colle ça :

YAML
name: CI/CD Pipeline K8s

on:
  push:
    branches: [ "main" ] # Se déclenche quand on push sur main

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Récupération du code
        uses: actions/checkout@v3

      - name: Connexion à Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build et Push de l'image Docker
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ${{ secrets.DOCKER_USERNAME }}/projetm1-api:latest
A la soutenance : Tu montreras que faire un simple git push active ce script, construit l'image, et l'envoie sur le web.

📊 3. GWENN : Le Monitoring et l'Observabilité
Ton rôle : Installer la tour de contrôle. Il nous faut Prometheus (la base de données de métriques) et Grafana (les tableaux de bord) pour surveiller la RAM/CPU de notre API sur Azure.

💻 Phase 1 : Pourquoi on n'a pas fait ça en local
A expliquer au prof : On a d'abord testé ta stack en local sur mon K3s avec Vagrant. Ça a été un carnage. Helm plantait. Grafana affichait le message "An error occurred within the plugin" et les courbes restaient sur "No data" parce que le PC n'avait plus de mémoire pour faire tourner l'outil kube-state-metrics. C'est techniquement impossible d'avoir un monitoring fiable sur une machine locale de 16 Go.

☁️ Phase 2 : L'installation sur Azure (Le succès)
Grâce au cluster Azure AKS, tu as la puissance nécessaire et plus aucun blocage de sécurité.

Tes commandes exactes à taper dans le terminal :

PowerShell
# 1. Tu ajoutes le "Play Store" de Prometheus à ton PC
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 2. Tu lances l'installation sur Azure (avec le LoadBalancer pour avoir une IP !)
helm install monitoring prometheus-community/kube-prometheus-stack `
  --namespace monitoring --create-namespace `
  --set grafana.service.type=LoadBalancer
Comment récupérer ton interface :
Tu attends 2 minutes, puis tu tapes ça pour demander l'IP publique à Azure :

PowerShell
kubectl get svc -n monitoring monitoring-grafana
Regarde la colonne EXTERNAL-IP. Tu copies cette adresse dans ton navigateur.

Tes identifiants :

User : admin

Password : prom-operator
