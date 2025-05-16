# Projet d'Algorithmie 2 (2024–2025) — Recherche du plus court chemin multimodal

Ce projet implémente un algorithme CSA (Connection Scan Algorithm) adapté aux réseaux de transport publics belges. Il permet à un utilisateur de déterminer l'itinéraire optimal entre deux arrêts de transport, en tenant compte des horaires de la STIB, SNCB, TEC et De Lijn, ainsi que des connexions à pied entre arrêts proches.

---

## Compilation du projet

Assurez-vous que Java 21 ou une version supérieure est installée sur votre système.

Pour compiler et créer le fichier `JAR` :

```sh
./gradlew clean jar
```

Le fichier exécutable sera généré à la racine du projet.

## Exécution

Voici la commande pour exécuter le programme :

```sh
java -jar algo2_project.jar
```

### Mémoire insuffisante : OutOfMemoryError

Si, lors de l’exécution, vous rencontrez une erreur du type :

```sh
java.lang.OutOfMemoryError: Java heap space
```

Cela signifie que la mémoire allouée par défaut à la JVM est insuffisante. Pour charger les fichiers GTFS de ce projet, au moins 3 Go de RAM sont recommandés.

Lancez donc le programme avec 3 Go de RAM alloués :

```sh
java -Xmx3g -jar algo2_project.jar
```

### Comportement du programme

Lors de l'exécution, le programme :

1. Charge les fichiers GTFS des quatre opérateurs depuis les dossiers `./GTFS/{SNCB,STIB,TEC,DELIJN}`.

2. Détermine les connexions à pied entre arrêts proches (500m max).

3. Invite l'utilisateur à saisir :

    - un nom d’arrêt de départ (le nom exact de l'arrêt en respectant les majuscules, minuscules et espaces),

    - un nom d’arrêt d’arrivée (le nom exact de l'arrêt en respectant les majuscules, minuscules et espaces),

    - une heure de départ (format `hh:mm:ss`).

4. Affiche le chemin calculé en détail avec toutes les instructions de connexions.

## Structure des fichiers GTFS

Les sous-dossiers portant le nom des opérauteurs se situant dans `./GTFS/` doivent contenir les 4 fichiers suivants :

- `routes.csv`

- `trips.csv`

- `stop_times.csv`

- `stops.csv`

Exemple de structure :

```sh
GTFS/
├── SNCB/
│   ├── routes.csv
│   ├── stop_times.csv
│   ├── stops.csv
│   └── trips.csv
├── STIB/
├── TEC/
└── DELIJN/
```

## Interactions utilisateur

L'utilisateur peut entrer `q` ou `quit` à tout moment pour quitter le programme.

Les noms d'arrêts ne sont pas sensibles à la casse. Pour chaque arrêt saisi, le programme va d'abord vérifier si l'arrêt existe dans les données chargées.

Le programme propose un *terminal interactif* : il est possible d’utiliser les flèches haut et bas pour naviguer dans l’historique des commandes, et les flèches gauche et droite pour modifier une ligne en cours de saisie.

## Librairies utilisées

- `Jline` pour les interactions terminal utilisateur
- `OpenCSV` pour le parsing des fichiers CSV
- `Util` pour les fonctions de base (List, Map, Stack, etc)
  