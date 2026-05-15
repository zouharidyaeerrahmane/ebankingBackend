# E-Banking Backend — Documentation

## Vue d'ensemble

API REST Spring Boot pour une application bancaire. Gère les clients, les comptes bancaires (courant/épargne), les opérations (débit, crédit, virement), la sécurité JWT, et un chatbot RAG connecté à Telegram.

---

## Stack technique

| Élément | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Base de données | MySQL 8 (MariaDB dialect) |
| Sécurité | Spring Security + OAuth2 Resource Server (JWT HS512) |
| Documentation | Springdoc OpenAPI (Swagger UI) |
| Chatbot LLM | Groq API (llama-3.3-70b-versatile) |
| Bot Telegram | telegrambots 7.3.0 (long polling) |
| Utilitaires | Lombok |

---

## Lancement

### Prérequis
- Java 21
- MySQL en cours d'exécution sur le port 3306

### Configuration

Copier le fichier d'exemple et renseigner les valeurs :

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

Variables à définir dans `application-local.properties` :

```properties
jwt.secret=<chaine_32_caracteres_minimum>
groq.api.key=<cle_api_groq>
telegram.bot.token=<token_botfather>
```

Le fichier `application.properties` contient déjà :
- Port serveur : **8084**
- Base de données : `bankdb` (créée automatiquement)
- DDL auto : `create` (la DB est recréée à chaque démarrage)

### Démarrage

```bash
./mvnw spring-boot:run
```

Swagger UI accessible sur : `http://localhost:8084/swagger-ui/index.html`

---

## Architecture des packages

```
org.java.enset.dezy.ebanking_backend
├── entities/          — entités JPA (BankAccount, Customer, AccountOperation…)
├── enums/             — AccountStatus, OperationType
├── dtos/              — objets de transfert (CustomerDTO, BankAccountDTO…)
├── mappers/           — conversion entité <-> DTO (BankAccountMapperImpl)
├── repositories/      — interfaces Spring Data JPA
├── services/          — logique métier (BankAccountService + Impl)
├── web/               — controllers REST
├── security/          — configuration JWT, SecurityConfig, SecurityController
└── chatbot/
    ├── config/        — configuration du chatbot
    ├── dto/           — ChatRequest, ChatResponse, GroqRequest, GroqResponse
    ├── service/       — pipeline RAG + bot Telegram
    └── web/           — ChatbotRestController
```

---

## Modèle de données

### Héritage JPA (SINGLE_TABLE)

```
BankAccount (abstract)
├── CurrentAccount   — overDraft (découvert autorisé)
└── SavingAccount    — interestRate (taux d'intérêt)
```

`BankAccount` est stockée dans une seule table avec colonne discriminante `TYPE`.

### Entités principales

| Entité | Champs clés |
|---|---|
| `Customer` | id (Long), name, email |
| `BankAccount` | id (String/UUID), balance, createdAt, status, customer |
| `AccountOperation` | id, operationDate, amount, type, description, bankAccount |

### Enums

- `AccountStatus` : `CREATED`, `ACTIVATED`, `SUSPENDED`
- `OperationType` : `DEBIT`, `CREDIT`

---

## API REST

### Authentification — `/auth`

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Retourne un JWT |
| GET | `/auth/profile` | Authentifié | Profil de l'utilisateur courant |

**Exemple de login :**
```
POST /auth/login
Content-Type: application/x-www-form-urlencoded

username=admin&password=admin0000
```
Réponse : `{ "access-token": "<jwt>" }`

**Durée du token : 10 minutes.**

---

### Clients — `/customers`

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| GET | `/customers` | USER | Liste tous les clients |
| GET | `/customers/search?keyword=` | USER | Recherche par nom |
| GET | `/customers/{id}` | USER | Détail d'un client |
| POST | `/customers` | ADMIN | Créer un client |
| PUT | `/customers/{id}` | ADMIN | Modifier un client |
| DELETE | `/customers/{id}` | ADMIN | Supprimer un client |

---

### Comptes bancaires — `/accounts`

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/accounts` | Liste tous les comptes |
| GET | `/accounts/{accountId}` | Détail d'un compte |
| GET | `/accounts/{accountId}/operations` | Historique complet |
| GET | `/accounts/{accountId}/pageOperations?page=0&size=5` | Historique paginé |
| POST | `/accounts/debit` | Débiter un compte |
| POST | `/accounts/credit` | Créditer un compte |
| POST | `/accounts/transfer` | Virement entre comptes |

**Corps pour débit/crédit :**
```json
{ "accountId": "...", "amount": 500.0, "description": "paiement" }
```

**Corps pour virement :**
```json
{ "accountSource": "...", "accountDestination": "...", "amount": 200.0 }
```

---

### Chatbot RAG — `/chatbot`

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| POST | `/chatbot/ask` | USER | Poser une question au chatbot |

**Corps :**
```json
{ "question": "Quelle est la différence entre courant et épargne ?", "accountId": "optionnel" }
```

---

## Sécurité

- Stateless (pas de session HTTP)
- JWT signé en **HS512**
- Tous les endpoints sauf `/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**` exigent un token valide
- Autorisation fine par rôle avec `@PreAuthorize`

**Utilisateurs en mémoire (InMemoryUserDetailsManager) :**

| Username | Password | Rôles |
|---|---|---|
| `user1` | `user1111` | USER |
| `admin` | `admin0000` | USER, ADMIN |

---

## Module Chatbot RAG

### Pipeline RAG (sans Spring AI)

```
Question utilisateur
      │
      ▼
1. RETRIEVE — DocumentSearchService
   Recherche par mots-clés dans les fichiers txt (src/main/resources/docs/)
      │
      ▼
2. AUGMENT — RagService
   Construit le prompt enrichi avec le contexte documentaire
   + données temps réel du compte (si accountId fourni)
      │
      ▼
3. GENERATE — GroqChatService
   Appel HTTP à l'API Groq (LLM llama-3.3-70b-versatile)
      │
      ▼
   Réponse texte
```

### Documents de base de connaissance

Situés dans `src/main/resources/docs/` :
- `banking_faq.txt` — questions fréquentes
- `banking_operations.txt` — description des opérations
- `banking_services.txt` — services bancaires disponibles

### Bot Telegram

Commandes disponibles :

| Commande | Effet |
|---|---|
| `/start` | Message de bienvenue |
| `/help` | Exemples de questions |
| `/compte <accountId>` | Infos temps réel sur un compte |
| `<texte libre>` | Question RAG générale |

---

## Exceptions gérées

| Exception | Déclenchée quand |
|---|---|
| `CustomerNotFoundException` | Client introuvable par son ID |
| `BankAccountNotFoundException` | Compte introuvable |
| `BalanceNotSufficientException` | Solde insuffisant pour un débit/virement |

---

## Configuration CORS

Toutes origines autorisées (`*`), toutes méthodes et headers autorisés. À restreindre en production.

## Realisé par :  ZOUHARI Dyae errahmane GLSID-2 

