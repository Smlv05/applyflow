# ApplyFlow

Backend for managing job applications: candidate profile, jobs, matching score,
applications and statistics. Portfolio project (Java / Spring Boot).

## Stack
Java 21, Spring Boot 4, Maven (wrapper), Spring Web / Data JPA / Security / Validation,
PostgreSQL 17 (Docker Compose). Planned: JWT, Testcontainers, Redis, RabbitMQ.

## Commands (Windows PowerShell)
- Start database: `docker compose up -d`
- Run app: `.\mvnw.cmd spring-boot:run`
- Tests: `.\mvnw.cmd test`
- Compile only: `.\mvnw.cmd compile`

## Architecture
Layered: controller -> service -> repository -> database.
Packages under `com.applyflow`: auth, user, job, application, matching, common, config.
Never return JPA entities from controllers; use DTOs.
Errors are handled globally with `@RestControllerAdvice`.

## Rules
- Do not commit secrets, passwords or keys.
- Do not edit or delete existing database migrations.
- Do not disable, skip or weaken tests to make a build pass.
- Do not add a dependency or technology without a concrete reason.
- Every user-owned resource must be checked against the authenticated user.
- Keep changes small: one task per commit, Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).

## Workflow
Branch per task -> Pull Request -> CI must pass -> merge.
AI-generated code is reviewed line by line, and every AI-assisted task is
recorded in `docs/ai-workflow/AI-LOG.md`.