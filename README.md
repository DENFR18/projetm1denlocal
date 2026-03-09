🏗️ Refonte Architecturale Complète : PaaS Cloud-Native & CI/CD
Salut l'équipe 👋,

Gros point technique aujourd'hui. Ce week-end, face aux limites de notre ancienne infrastructure (la VM Vagrant qui saturait la RAM et plantait sans arrêt), j'ai pris la décision de tout reprendre de zéro.

L'objectif n'était pas de faire un simple patch, mais de concevoir une véritable architecture logicielle et DevOps de niveau production. J'ai développé un backend complet, mis en place l'orchestration Kubernetes, réparé notre dépôt Git et créé une chaîne CI/CD entièrement automatisée.

Prenez le temps de lire ce document en entier. Il explique toute la mécanique du projet que j'ai monté, dossier par dossier, pour qu'on puisse ensuite le déployer ensemble sur le Cloud public.

📂 1. La Nouvelle Architecture du Projet (Deep Dive)
Fini le code en vrac. J'ai repensé toute l'arborescence pour séparer proprement la logique métier, le frontend, l'infrastructure et l'automatisation. Voici la structure exacte que j'ai mise en place sur le dépôt :

Plaintext
PROJET M1/
├── .github/workflows/       # 🤖 CI/CD : L'usine logicielle
│   └── cicd.yaml            # Le pipeline GitHub Actions
├── api-backend/             # 🧠 LE CŒUR DU SYSTÈME (Spring Boot)
│   ├── src/main/java/com/example/
│   │   ├── controller/      # API Rest : Gère les requêtes HTTP du front
│   │   ├── model/           # Définition des objets de données
│   │   ├── repository/      # Couche d'accès aux données (logs, histo)
│   │   ├── service/         # Logique métier et orchestration K8s
│   │   │   └── DeploymentService.java  # Le lien direct avec l'API K8s
│   │   └── App.java         # Point d'entrée de l'application Java
│   ├── src/main/resources/static/
│   │   └── index.html       # 🌐 Frontend : Interface de soumission de code
│   ├── target/              # Fichiers compilés par Maven (.jar, classes)
│   ├── dockerfile           # Recette de conteneurisation du backend
│   └── pom.xml              # Gestion des dépendances Java (Maven)
├── infra/                   # ⚙️ INFRASTRUCTURE & DÉPLOIEMENT
│   ├── deployment.yaml      # Manifestes Kubernetes (Déploiements, Services)
│   ├── eksctl.exe / helm.exe # Outils CLI (Désormais bloqués par Git)
│   └── Vagrantfile          # Vestige de l'ancienne VM
├── .gitignore               # 🛡️ Filtre de sécurité Git
└── dockerfile / pom.xml     # Fichiers root de configuration
🧠 2. Le Backend Java (Le Moteur d'Orchestration)
C'est ici que j'ai passé le plus de temps. J'ai codé une API REST en Java 17 avec le framework Spring Boot. Ce n'est pas juste un serveur web, c'est un véritable orchestrateur.

Comment ça marche sous le capot ? (Architecture MVC)
L'Interface Utilisateur (index.html) : L'utilisateur choisit son langage (Node.js, Python, Java) et tape son code. Au clic sur "Lancer", le code est envoyé en JSON à notre backend.

Le Routeur (controller/) : Il intercepte la requête, vérifie qu'elle n'est pas vide, et la transmet à la couche de service.

L'Intelligence (service/DeploymentService.java) : C'est la pièce maîtresse du projet. Ce fichier Java utilise le client officiel Kubernetes.

Il génère dynamiquement un conteneur éphémère (Pod) spécifique au langage choisi.

Il injecte le code de l'utilisateur à l'intérieur.

Il surveille l'état du Pod. Si le téléchargement de l'image (Pull) prend plus de 20 secondes, il gère proprement un "Timeout" pour ne pas bloquer le serveur.

Une fois le code exécuté, il extrait les logs (le résultat) et ordonne à Kubernetes de détruire le Pod pour libérer les ressources.

Le Stockage (model/ & repository/) : Ces dossiers sont préparés pour sauvegarder l'historique des exécutions (pour pouvoir relier ça à une base de données SQL plus tard).

💥 3. Le Crash Git et la mise en place de la CI/CD
Le Problème : Le dépôt Git était mort
En regardant le dossier infra/, vous verrez eksctl.exe et helm.exe. Ces binaires pèsent des centaines de mégaoctets (jusqu'à 144 Mo pour eksctl). En essayant de les versionner, on a complètement saturé Git et GitHub refusait nos push.

La Solution : Le grand nettoyage
J'ai dû réécrire l'historique Git en profondeur pour purger ces fichiers. Ensuite, j'ai créé un .gitignore ultra strict pour être sûr que ça n'arrive plus jamais :

Plaintext
# Fichiers compilés et exécutables lourds
target/
*.jar
*.exe
*.dll

# IDE et OS
.idea/
*.iml
.vscode/
.DS_Store
L'Automatisation : GitHub Actions (cicd.yaml)
Une fois le repo propre, j'ai codé l'usine logicielle. À chaque fois qu'on pousse sur la branche main, un serveur s'allume, installe Java 17, télécharge Maven, compile tout le dossier api-backend, construit l'image Docker, s'authentifie sur Docker Hub et la met en ligne.

Voici le code complet de notre pipeline :

YAML
name: CI/CD Pipeline API PaaS

on:
  push:
    branches:
      - main

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Récupération du code
        uses: actions/checkout@v3

      - name: Configuration de Java 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Compilation et empaquetage (Maven)
        run: mvn clean package -DskipTests

      - name: Connexion à Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build et Push de l'image Docker
        uses: docker/build-push-action@v4
        with:
          context: ./api-backend
          push: true
          tags: den95/projetm1-api:latest
(Pour info : le dernier build complet de tout le backend jusqu'à la publication a pris 1 minute et 16 secondes chronomètre en main).

📊 4. La Tour de Contrôle : Observabilité et Monitoring
Faire tourner des pods, c'est bien. Vérifier que l'infra tient la route, c'est mieux.
J'ai déployé en local la stack kube-prometheus-stack via Helm dans le namespace monitoring.

Le Stress-Test K8s
Pour prouver que Prometheus et Grafana détectent bien l'activité de notre API Java, j'ai écrit un script Node.js intensif que j'ai exécuté depuis notre portail web index.html.

Le Script "Mode BRRR" (Surcharge CPU) :

JavaScript
console.log("🔥 Démarrage du mode BRRR...");
const duration = 60000; // 60 secondes de calcul à 100% CPU
const start = Date.now();
let operations = 0;

// Boucle mathématique asynchrone pour saturer le Pod
while (Date.now() - start < duration) {
    Math.sqrt(Math.random() * 1000) * Math.atan(Math.random());
    operations++;
    if (operations % 5000000 === 0) {
        console.log(`Statut : ${operations} opérations...`);
    }
}
console.log("✅ Stress-test terminé. Le cluster a bien transpiré !");
Le Résultat : L'API a créé le Pod, et Grafana a instantanément affiché le pic de charge CPU et l'augmentation des quotas dans ses graphiques. Le pont entre le code, K8s et le monitoring est 100% opérationnel.

(Petite galère technique résolue : j'ai dû décoder les Secrets K8s en Base64 pour récupérer le mot de passe admin de Grafana, et monter un port-forward réseau pour exposer l'interface).

🚀 5. Comment reproduire cette Infra sur vos PC
Le code est en ligne. Pour tester ça chez vous et avoir la même puissance de feu que moi, voici la procédure :

Assurez-vous d'avoir activé Kubernetes dans Docker Desktop.

Récupérez les dernières modifs via git pull.

Pour vérifier que l'API est bien déployée par Kubernetes (via l'image Docker Hub) :

Bash
kubectl get pods -A
Pour accéder à la tour de contrôle Grafana (laissez ce terminal ouvert) :

Bash
kubectl port-forward svc/monitoring-grafana 8081:80 -n monitoring
(Rendez-vous sur http://localhost:8081 - venez me voir en privé pour les identifiants admin).

🎯 La Feuille de Route
L'architecture locale (Proof of Concept) est maintenant un succès total.
La suite logique pour le projet final :

Prendre notre manifeste dans le dossier infra/deployment.yaml.

Le pousser sur un véritable cluster Cloud Managé (Amazon EKS ou Azure AKS).

Connecter notre pipeline GitHub Actions directement à ce Cloud public pour que le déploiement se fasse en live.

Prenez le temps d'explorer l'arborescence, en particulier la mécanique MVC dans le dossier api-backend. Dites-moi quand vous avez cloné tout ça et on s'organise une synchro pour que je vous montre comment lancer les tests ! ✌️
