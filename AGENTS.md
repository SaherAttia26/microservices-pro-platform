# Repository Guidelines

## Project Structure & Module Organization

This is an e-commerce microservices training platform. Business services live in `services/` (`product-service`, `order-service`, `inventory-service`, `payment-service`, and `notification-service`). Shared platform components live in `infrastructure/`, including `api-gateway`, `config-server`, and `eureka-server`. Each module is an independent Maven/Spring Boot project with code in `src/main/java`, configuration in `src/main/resources`, and tests in `src/test/java`.

Use `docs/architecture/` for intended boundaries and `docs/labs/` for session scope. `docker-compose.yml` starts supporting infrastructure such as PostgreSQL, Redis, and Kafka.

## Build, Test, and Development Commands

Run commands from the module being changed. On Windows, use the wrapper:

```powershell
cd services/order-service
.\mvnw.cmd test          # compile and run unit tests
.\mvnw.cmd spring-boot:run # start one service locally
.\mvnw.cmd clean package  # create the executable JAR
```

Use `docker compose up --build` to start the containerized platform when its configuration is in scope. Start local services in dependency order: config server, Eureka/discovery server, gateway, then business services.

## Coding Style & Naming Conventions

Write Java 21 code and follow the surrounding Spring conventions: four-space indentation, one public type per file, and package names such as `com.raya.order_service`. Keep layers explicit: `controller`, `service`, `repository`, `dto`, `model`, `config`, and `messaging`/`saga` as appropriate. Use PascalCase for classes and records, camelCase for methods and fields, and descriptive request/response DTO names (for example, `PaymentRequest`). Match the existing code; no project-wide formatter or linter is configured.

## Testing Guidelines

Tests use JUnit 5, Spring Boot Test, and Mockito. Place tests in the matching package under `src/test/java`; name classes `*Test` or `*ApplicationTests` and use behavior-oriented methods such as `checkStock_returnsAvailable_whenSufficientStock`. Run `mvn test` for every affected module before committing. The CI workflow currently tests product service and API gateway and compiles the infrastructure servers; all changed modules still need local verification.

## Commit & Pull Request Guidelines

Commit subjects are enforced as `session-NN: short-description`, for example `session-05: add-bulkhead-to-payment-client`. Keep each commit within the current lab/session; do not introduce later-session infrastructure or unnecessary dependencies. PRs should explain the session goal, list verification commands/results, link the relevant issue or lab when available, and include screenshots or sample requests for user-visible API or gateway changes.

## Configuration & Security

Do not commit credentials or environment-specific secrets. Clearly mark any local-only configuration in YAML (for example, `# DEV ONLY`) and preserve production-safe defaults.
