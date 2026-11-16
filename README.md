# Sprint Tracker

Developer-focused, terminal-first sprint and issue tracker. Multi-module Maven
project built on Java 21, Spring Boot 3.4, PostgreSQL and a Picocli CLI.

## Modules

| Module           | Description                                                        |
|------------------|--------------------------------------------------------------------|
| `tracker-common` | Shared DTOs, enums and validation contracts                        |
| `tracker-api`    | Stateless REST API (Spring Boot, JPA, Flyway, JWT, OpenAPI)        |
| `tracker-cli`    | Terminal client (Picocli + `java.net.http.HttpClient`)             |

## Prerequisites

- Java 21 (the build targets `--release 21`)
- Maven 3.9+
- Docker and Docker Compose

## Quick start

Start PostgreSQL (and optionally the API container):

```bash
docker compose up -d db
# or: docker compose up --build
```

Run the API locally against the containerized database:

```bash
mvn clean install
mvn -pl tracker-api spring-boot:run
```

- API: <http://localhost:8080>
- OpenAPI UI: <http://localhost:8080/swagger-ui.html>

Build and run the CLI:

```bash
mvn -pl tracker-cli -am package
java -jar tracker-cli/target/tracker.jar --help
```

## Environment variables

| Variable                 | Default                                             | Purpose                     |
|--------------------------|-----------------------------------------------------|-----------------------------|
| `SERVER_PORT`            | `8080`                                              | API HTTP port               |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/tracker`          | PostgreSQL JDBC URL         |
| `DB_USERNAME`            | `tracker`                                           | Database user               |
| `DB_PASSWORD`            | `tracker`                                           | Database password           |
| `JWT_SECRET`             | development placeholder                             | JWT signing secret          |
| `JWT_EXPIRATION_MINUTES` | `60`                                                | Token lifetime              |

Copy `.env.example` to `.env` to override Compose defaults.

## Project layout

```text
sprint-ledger/
├── docker-compose.yml
├── pom.xml
├── tracker-common/   # shared contracts
├── tracker-api/      # REST API, Flyway migrations, Dockerfile
└── tracker-cli/      # Picocli commands
```

## Roadmap

- [x] Milestone 1 — Project scaffolding and Docker
- [ ] Milestone 2 — Shared contracts and database schema
- [ ] Milestone 3 — Core services and business rules
- [ ] Milestone 4 — REST controllers, security and OpenAPI
- [ ] Milestone 5 — Testcontainers integration tests
- [ ] Milestone 6 — Picocli CLI client
