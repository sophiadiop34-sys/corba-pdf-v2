# 🗂️ CORBA PDF Service

> **Système Distribué de Manipulation de Fichiers PDF**  
> Architecture CORBA • Spring Boot • Apache PDFBox • Docker

---

## 📌 Présentation

**CORBA PDF Service** est un système distribué complet de manipulation de fichiers PDF développé dans le cadre d'un projet académique. Il implémente l'architecture **CORBA** (Common Object Request Broker Architecture) avec une interface web moderne, permettant d'effectuer plus de **15 opérations PDF** via des appels distants.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Navigateur Web                       │
│              (Bootstrap 5 + JavaScript)                 │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/REST
┌────────────────────────▼────────────────────────────────┐
│              Spring Boot (Tomcat embarqué)               │
│         Controller REST  ←→  Thymeleaf Templates        │
└────────────────────────┬────────────────────────────────┘
                         │ Appel CORBA (ORB/POA)
┌────────────────────────▼────────────────────────────────┐
│              Servant CORBA (PDFServiceImpl)              │
│                 POA + ORB Java 8 natif                  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Apache PDFBox 2.x                          │
│      Manipulation réelle des fichiers PDF               │
└─────────────────────────────────────────────────────────┘
```

### Composants CORBA

| Composant | Rôle |
|-----------|------|
| `PDFService.idl` | Interface Definition Language — contrat CORBA |
| `CORBAServer` | Initialise l'ORB et enregistre le servant dans le POA |
| `PDFServiceImpl` | Servant — implémente les 15 méthodes de l'IDL |
| `PdfService` | Couche Spring — pont entre HTTP et CORBA |

---

## 🧩 Technologies

| Technologie | Version | Usage |
|------------|---------|-------|
| ☕ Java | 8 (LTS) | Langage principal, CORBA natif |
| 🔗 CORBA/ORB | Java 8 built-in | Communication distribuée |
| 🌱 Spring Boot | 2.7.18 | Framework web + IoC |
| 📄 Apache PDFBox | 2.0.29 | Manipulation PDF |
| 🍃 Thymeleaf | 3.x | Templating HTML |
| 🎨 Bootstrap | 5.3 | Interface utilisateur |
| 🔧 Maven | 3.8+ | Build et dépendances |
| 🐳 Docker | 20+ | Conteneurisation |

---

## 📋 Fonctionnalités

### 🗂️ Gestion PDF
- ✅ **Fusion** — Fusionner plusieurs PDFs en un seul
- ✅ **Découpage** — Extraire chaque page individuellement
- ✅ **Extraction de pages** — Extraire une plage de pages
- ✅ **Suppression de pages** — Supprimer des pages spécifiques
- ✅ **Réorganisation** — Changer l'ordre des pages
- ✅ **Rotation** — Pivoter des pages (90°, 180°, 270°)

### 🔍 Analyse PDF
- ✅ **Extraction de texte** — Récupérer tout le contenu textuel
- ✅ **Recherche** — Trouver un mot/phrase avec contexte et page
- ✅ **Statistiques** — Pages, mots, taille, auteur, titre, date

### 🔒 Sécurité & Transformation
- ✅ **Mot de passe** — Chiffrement AES-128 (utilisateur + propriétaire)
- ✅ **Filigrane** — Texte diagonal transparent sur toutes les pages
- ✅ **Compression** — Réduction de la taille du fichier
- ✅ **PDF → Image** — Conversion d'une page en PNG (jusqu'à 300 DPI)

### 📝 Création
- ✅ **Nouveau PDF** — Depuis du texte, avec mise en page professionnelle

---

## 🚀 Installation & Lancement

### Prérequis
- Docker Desktop installé
- Docker Compose disponible

### Démarrage en une commande

```bash
# Cloner / télécharger le projet
cd corba-pdf-project

# Lancer le projet (build + démarrage)
docker-compose up --build
```

⏳ Le premier build prend environ **2-3 minutes** (téléchargement des dépendances Maven).

### Accès à l'interface

Une fois démarré, ouvrez votre navigateur :

```
http://localhost:8080
```

### Vérifier que tout fonctionne

```bash
# Statut du serveur CORBA
curl http://localhost:8080/api/pdf/status

# Réponse attendue :
# {"success":true,"message":"Serveur opérationnel","data":{...}}
```

---

## 📂 Structure du Projet

```
corba-pdf-project/
│
├── 📄 Dockerfile                    # Image Docker multi-stage
├── 📄 docker-compose.yml            # Orchestration des services
├── 📄 pom.xml                       # Configuration Maven
├── 📄 README.md                     # Ce fichier
│
├── 📁 idl/
│   └── PDFService.idl               # Interface CORBA (IDL complet)
│
└── 📁 src/main/
    ├── 📁 java/com/pdfcorba/
    │   ├── Application.java          # Point d'entrée Spring Boot
    │   ├── 📁 corba/
    │   │   ├── PDFServiceModule.java # Stubs CORBA (équivalent idlj)
    │   │   ├── PDFServiceImpl.java   # Servant CORBA + PDFBox
    │   │   └── CORBAServer.java      # Initialisation ORB + POA
    │   ├── 📁 service/
    │   │   └── PdfService.java       # Couche service Spring
    │   ├── 📁 controller/
    │   │   ├── PdfController.java    # API REST
    │   │   └── WebController.java    # Pages HTML
    │   └── 📁 model/
    │       └── ApiModels.java        # DTOs de réponse
    │
    └── 📁 resources/
        ├── application.properties    # Configuration
        ├── 📁 templates/             # Pages HTML Thymeleaf
        │   ├── index.html            # Accueil
        │   ├── manage.html           # Gestion PDF
        │   ├── analyze.html          # Analyse PDF
        │   ├── transform.html        # Sécurité & Transformation
        │   └── create.html           # Création PDF
        └── 📁 static/
            ├── css/style.css         # Styles CSS
            └── js/app.js             # JavaScript client
```

---

## 🌐 API REST — Endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/pdf/status` | Statut du serveur CORBA |
| POST | `/api/pdf/merge` | Fusionner des PDFs |
| POST | `/api/pdf/split` | Découper un PDF |
| POST | `/api/pdf/extract-pages` | Extraire des pages |
| POST | `/api/pdf/delete-pages` | Supprimer des pages |
| POST | `/api/pdf/reorder` | Réorganiser les pages |
| POST | `/api/pdf/rotate` | Rotation des pages |
| POST | `/api/pdf/extract-text` | Extraire le texte |
| POST | `/api/pdf/search` | Rechercher dans le PDF |
| POST | `/api/pdf/info` | Statistiques du PDF |
| POST | `/api/pdf/protect` | Ajouter un mot de passe |
| POST | `/api/pdf/watermark` | Ajouter un filigrane |
| POST | `/api/pdf/compress` | Compresser le PDF |
| POST | `/api/pdf/to-image` | Convertir en image PNG |
| POST | `/api/pdf/create` | Créer un nouveau PDF |
| GET | `/api/pdf/download/{id}` | Télécharger un résultat |

---

## 🧪 Tests des Fonctionnalités

### Via l'interface web
1. Ouvrez `http://localhost:8080`
2. Naviguez vers la section souhaitée
3. Déposez votre PDF dans la zone de dépôt
4. Configurez les options et cliquez sur l'action

### Via cURL (ligne de commande)

```bash
# Informations d'un PDF
curl -X POST http://localhost:8080/api/pdf/info \
  -F "file=@monDocument.pdf"

# Extraction de texte
curl -X POST http://localhost:8080/api/pdf/extract-text \
  -F "file=@monDocument.pdf"

# Fusion de deux PDFs
curl -X POST http://localhost:8080/api/pdf/merge \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf" \
  -o resultat_fusion.pdf

# Création d'un PDF
curl -X POST http://localhost:8080/api/pdf/create \
  -F "title=Mon Document" \
  -F "content=Contenu du document..." \
  -F "author=Jean Dupont" \
  -o nouveau_document.pdf
```

---

## 🛑 Commandes Docker Utiles

```bash
# Démarrer en arrière-plan
docker-compose up -d --build

# Voir les logs en temps réel
docker-compose logs -f

# Arrêter le service
docker-compose down

# Reconstruire sans cache
docker-compose build --no-cache

# Voir le statut des conteneurs
docker-compose ps
```

---

## 📘 Fichier IDL CORBA

Le fichier `idl/PDFService.idl` définit l'interface complète du service :

```idl
module PDFServiceModule {
    exception PDFException { string message; };

    struct PDFInfo { ... };
    struct SearchResult { ... };

    interface PDFService {
        ByteSequence mergePDFs(in ByteSeqList pdfFiles)    raises (PDFException);
        ByteSeqList  splitPDF(in ByteSequence pdfData)     raises (PDFException);
        string       extractText(in ByteSequence pdfData)  raises (PDFException);
        ByteSequence addWatermark(...)                     raises (PDFException);
        // ... 11 autres méthodes
    };
};
```

En Java 8, les stubs CORBA sont normalement générés via `idlj -fall PDFService.idl`.  
Dans ce projet, ils sont fournis directement dans `PDFServiceModule.java`.

---

## 👨‍🎓 Informations Académiques

- **Architecture** : Systèmes Distribués — CORBA
- **Framework** : Spring Boot 2.7 (Java 8)
- **Bibliothèque PDF** : Apache PDFBox 2.0.29
- **Déploiement** : Docker multi-stage build

---

*CORBA PDF Service — Projet Académique*
