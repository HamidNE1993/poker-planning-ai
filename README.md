# Poker Planning AI

Monorepo Maven : Java 25 / Spring Boot 4.1.1 + Angular 22.2.

### Backend

```bash
mvn -pl backend spring-boot:run
```

Ou lancer `PokerPlanningApplication` depuis IntelliJ.

- API health : `http://localhost:8080/api/health`
- Console H2 : `http://localhost:8080/h2-console`
- JDBC URL : `jdbc:h2:file:./data/poker-planning`
- User : `sa`
- Password : vide

Les données H2 sont persistées dans `backend/data/` si le backend est lancé depuis le dossier backend (le chemin reste relatif au working directory).

### Frontend

```bash
cd frontend
npm install
npm start
```

Frontend : `http://localhost:4200`

## Build Maven

```bash
mvn clean verify
```

## Production avec PostgreSQL

Activer le profil `prod` (ex: `SPRING_PROFILES_ACTIVE=prod`).

Le backend détecte automatiquement :
- L'URL standard Railway / Heroku (`DATABASE_URL`, `POSTGRES_URL`)
- Ou les variables classiques : `DB_URL` (ou `PGHOST`, `PGPORT`, `PGDATABASE`), `DB_USERNAME`, `DB_PASSWORD`.

Exemple :

```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Déploiement Cloud (Railway)

Un guide complet de mise en production pas à pas sur Railway est disponible dans [RAILWAY_DEPLOY.md](RAILWAY_DEPLOY.md).

- **Backend** : `backend/Dockerfile` + `backend/railway.toml` (Healthcheck sur `/actuator/health`).
- **Frontend** : `frontend/Dockerfile` + `frontend/railway.toml` + Nginx avec reverse-proxy et support SSE.
- **Base de données** : PostgreSQL managé Railway provisionné en 1 clic.

## Docker & Docker Compose

Pour exécuter la stack complète (PostgreSQL 18 + Backend + Frontend) en local :

```bash
docker compose up --build
```
