# AGENTS.md — Poker Planning AI

## Project purpose

Poker Planning AI is a monorepo application for collaborative Planning Poker enhanced with AI.

The AI assists the team by analyzing user stories, identifying ambiguity and risks, suggesting clarification questions, and proposing explainable estimates. It must assist decision-making, not replace the team's final estimation.

## Tech stack

### Backend
- Java 25
- Spring Boot 4.1.x
- Maven
- Spring Web
- Spring Data JPA
- Jakarta Validation
- H2 for local development
- PostgreSQL for production
- JUnit 5

### Frontend
- Angular 22
- TypeScript 6
- RxJS
- Standalone Angular APIs
- Maven integration for the global build

## Repository structure

```text
/
├── pom.xml
├── AGENTS.md
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/
└── frontend/
    ├── pom.xml
    ├── package.json
    ├── angular.json
    └── src/
```

Do not introduce Docker files or Docker dependencies unless explicitly requested.

## Build and run

### Full project

```bash
mvn clean verify
```

### Backend

```bash
mvn -pl backend spring-boot:run
```

The default Spring profile is `local`.

### Frontend

```bash
cd frontend
npm install
npm start
```

Default URLs:

- Angular: `http://localhost:4200`
- Backend: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`

## Database profiles

### Local

Use H2. Local development must work without Docker and without a locally installed PostgreSQL server.

Expected JDBC URL:

```text
jdbc:h2:file:./data/poker-planning
```

Do not add PostgreSQL-specific SQL to the local path unless there is a compatible alternative.

### Production

Use PostgreSQL through environment variables. Never hardcode production credentials.

Example properties:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## Backend architecture

Prefer a feature/domain-oriented architecture with clear dependency boundaries:

```text
com.pokerplanning
├── configuration
├── session
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── story
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── estimation
├── participant
└── ai
```

Rules:

- Controllers handle HTTP concerns only.
- Business rules belong in the domain/application layers.
- Persistence concerns stay in infrastructure.
- Do not expose JPA entities directly through REST APIs.
- Use explicit request/response DTOs.
- Validate API inputs with Jakarta Validation.
- Keep transactions at application/service boundaries.
- Prefer constructor injection.
- Avoid field injection.
- Avoid static mutable state.
- Do not create generic `Utils`, `Helper`, or catch-all service classes.
- Keep classes focused on one responsibility.

## REST API conventions

All application endpoints use:

```text
/api/...
```

Use appropriate HTTP semantics:

- `GET` for reads
- `POST` for creation/actions
- `PUT` for full replacement when appropriate
- `PATCH` for partial updates
- `DELETE` for deletion

Return meaningful HTTP status codes.

Use a centralized exception handler (`@RestControllerAdvice`) for API errors.

Keep error responses structured and stable.

## Angular architecture

Organize Angular primarily by feature:

```text
src/app/
├── core/
├── shared/
└── features/
    ├── sessions/
    ├── stories/
    ├── planning-poker/
    └── ai-assistant/
```

Rules:

- Use standalone components.
- Prefer Angular's current built-in control flow (`@if`, `@for`, etc.).
- Prefer signals for local synchronous UI state when appropriate.
- Use RxJS for asynchronous streams and event composition.
- Keep HTTP access in dedicated services.
- Components should orchestrate presentation, not contain business logic.
- Use typed interfaces/models; avoid `any`.
- Use lazy-loaded feature routes when useful.
- Keep reusable UI components independent of business features.

## Coding standards

### Java

- Use Java 25 language features when they improve clarity.
- Prefer records for immutable DTOs/value carriers when appropriate.
- Prefer immutable objects.
- Use meaningful domain names.
- Avoid unnecessary abstractions.
- Do not add Lombok unless explicitly requested.
- Do not suppress warnings merely to make builds green.
- Keep public APIs small.
- Add comments only when the reason behind code is not obvious.

### TypeScript

- Keep strict mode enabled.
- Never introduce `any` to bypass typing problems.
- Prefer `unknown` when input type is genuinely unknown.
- Keep functions small and focused.
- Prefer immutable transformations.
- Do not manually subscribe when Angular/RxJS offers a lifecycle-safe alternative.

## Testing

Every meaningful business rule should have automated tests.

Backend:

- Unit-test domain/application logic.
- Use integration tests for repositories and HTTP boundaries when valuable.
- Do not require external infrastructure for ordinary local tests.

Frontend:

- Test business-relevant component/service behavior.
- Avoid tests that merely duplicate Angular implementation details.

A change is not complete if existing tests fail.

## AI integration principles

AI functionality belongs behind an application-level abstraction.

Example:

```java
public interface StoryAnalysisService {
    StoryAnalysis analyze(UserStory story);
}
```

Infrastructure implementations may later use Spring AI or another provider.

Do not couple domain objects directly to an LLM SDK.

AI output must be treated as untrusted external input:

- validate structured responses;
- handle timeouts and provider failures;
- define fallback behavior;
- avoid putting secrets or unnecessary personal data into prompts;
- log metadata rather than sensitive prompt content where possible.

AI estimates are advisory. The final Planning Poker estimate remains a team decision.

Future AI capabilities may include:

- story analysis;
- ambiguity detection;
- missing acceptance criteria;
- clarification questions;
- risk/dependency detection;
- estimation suggestions;
- similar-story retrieval;
- RAG;
- pgvector;
- tools/skills;
- agents;
- evaluation and observability.

Do not introduce these prematurely. Implement them incrementally.

## Security

- Never commit passwords, API keys, tokens, or production credentials.
- Use environment variables for secrets.
- Validate all external input.
- Do not expose stack traces through REST responses.
- Apply least privilege to future integrations.
- Treat LLM responses as untrusted data.

## Dependency policy

Before adding a dependency:

1. Check whether Java, Spring Boot, or Angular already provides the capability.
2. Add third-party dependencies only when they provide clear value.
3. Keep versions centralized where practical.
4. Do not downgrade Java, Spring Boot, Angular, or TypeScript without explicit approval.
5. Do not introduce Docker as a local development requirement.

## Change rules for coding agents

Before modifying code:

1. Inspect the relevant existing files.
2. Understand the current architecture and conventions.
3. Make the smallest coherent change that solves the requirement.
4. Preserve backward compatibility unless the task explicitly requires a breaking change.
5. Update tests together with behavior changes.
6. Run the relevant build/tests when the environment allows it.

Do not:

- rewrite unrelated code;
- rename public APIs without a reason;
- generate placeholder abstractions for hypothetical future needs;
- duplicate existing functionality;
- silently change framework versions;
- add Docker;
- replace H2 as the default local database.

After a substantial change, report:

- what changed;
- important technical decisions;
- tests/build executed;
- anything that could not be verified.

## Current implementation priority

Build the application incrementally in this order:

1. Project foundation
2. Planning session
3. Participants
4. User stories
5. Fibonacci voting
6. Vote reveal and consensus
7. Real-time collaboration
8. AI story analysis
9. Historical estimation
10. RAG / advanced AI capabilities

Favor a clean, working vertical slice over implementing many incomplete features.
