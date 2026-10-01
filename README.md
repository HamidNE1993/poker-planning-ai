# Poker Planning AI

Monorepo Maven : Java 25 / Spring Boot 4.1.1 + Angular 22.2.

## Développement sur le PC d’entreprise (sans Docker)

Le profil `local` est actif par défaut et utilise une base H2 persistante. Aucune installation PostgreSQL ou Docker n’est nécessaire.

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

Activer le profil `prod` et fournir :

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Exemple :

```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Docker

Les fichiers Docker/Compose sont conservés pour CI/CD ou une autre machine. Ils ne sont pas nécessaires pour le développement local.
