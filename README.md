# FileChunkingSystem

📌 Description

FileChunkingSystem est une application de gestion de fichiers permettant de :
	•	Diviser des fichiers en chunks avec FastCDC
	•	Dédupliquer les chunks pour éviter la redondance
	•	Compresser les chunks pour optimiser le stockage
	•	Stocker les chunks dans une base de données SQLite
	•	Utiliser JMH pour benchmarker la performance du chunking, de la compression et de la déduplication


🚀 Installation

1️⃣ Cloner le projet

git clone https://github.com/votre-repo/FileChunkingSystem.git
cd FileChunkingSystem

2️⃣ Compiler et générer le JAR

mvn clean package

Le fichier exécutable est généré dans target/FileChunkingSystem-1.0-SNAPSHOT.jar.

🔥 Exécution de l’application

Démarrer l’application Spring Boot

mvn clean install                                                            
mvn spring-boot:run

L’API sera disponible sur http://localhost:8080.

1️⃣ Upload un fichier pour traitement

curl -X POST "http://localhost:8080/api/process-file" -F "file=@/chemin/vers/fichier.txt"

2️⃣ Récupérer tous les chunks stockés

curl -X GET "http://localhost:8080/api/chunks"

⚡ Exécuter le Benchmark

Le benchmark utilise JMH pour évaluer la performance des opérations.

1️⃣ Générer le package avec JMH

mvn clean package

2️⃣ Lancer le benchmark

java -jar target/FileChunkingSystem-1.0-SNAPSHOT.jar

⚠️ Par défaut, cela exécutera tous les benchmarks.

3️⃣ Exécuter un benchmark spécifique

java -jar target/FileChunkingSystem-1.0-SNAPSHOT.jar org.example.benchmark.FileProcessingBenchmark

🔍 Structure du Projet

FileChunkingSystem/
│── src/
│   ├── main/java/org/example/
│   │   ├── chunking/         # Algorithmes de découpage (FastCDC, Rabin)
│   │   ├── compression/      # Compression des chunks
│   │   ├── deduplication/    # Détection de doublons
│   │   ├── model/            # Définition des entités (Chunk)
│   │   ├── repository/       # Gestion des chunks en base de données
│   │   ├── service/          # Logique métier
│   │   ├── controller/       # API REST
│   ├── test/                 # Tests unitaires et d’intégration
│── libs/                     # Dépendances locales (JMH, FastCDC)
│── target/                   # Fichiers générés après compilation
│── pom.xml                    # Configuration Maven
│── README.md                  # Documentation

