# Project Specification: Sprint & Issue Tracker (Java Spring Boot + Picocli CLI)

## Overview
Build a developer-focused, terminal-first Jira-style sprint and issue tracking system. The system consists of a multi-module Maven project containing:
1. **`tracker-common`**: Shared DTOs, enums, and validation contracts.
2. **`tracker-api`**: A stateless REST API built with Java 21 and Spring Boot 3.4+, backed by PostgreSQL and Flyway migrations.
3. **`tracker-cli`**: A command-line client built with Picocli and Java `HttpClient` for fast terminal workflows.

---

## Tech Stack & Dependencies
* **Language:** Java 21 (LTS)
* **Build Tool:** Maven (Multi-module project)
* **Backend Framework:** Spring Boot 3.4+ (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`)
* **Database:** PostgreSQL 16+
* **Database Migrations:** Flyway (`flyway-core`, `flyway-database-postgresql`)
* **Authentication:** Stateless JWT (`jjwt` or Spring Security OAuth2 Resource Server)
* **API Documentation:** Springdoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
* **CLI Framework:** Picocli (`info.picocli:picocli`)
* **Testing:** JUnit 5, Mockito, Testcontainers (`org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`)
* **Containerization:** Docker & Docker Compose

---

## Repository & Module Structure

```text
sprint-tracker/
├── docker-compose.yml
├── pom.xml
├── README.md
├── tracker-common/
│   ├── pom.xml
│   └── src/main/java/com/tracker/common/
│       ├── dto/                # Request/Response records (ProjectDto, SprintDto, IssueDto, AuthDto)
│       └── enums/              # SprintStatus, IssueStatus, IssuePriority, UserRole
├── tracker-api/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/tracker/api/
│       │   │   ├── TrackerApiApplication.java
│       │   │   ├── config/         # SecurityConfig, JwtFilter, OpenApiConfig, AsyncConfig
│       │   │   ├── controller/     # AuthController, ProjectController, SprintController, IssueController
│       │   │   ├── service/        # AuthService, ProjectService, SprintService, IssueService, AuditService
│       │   │   ├── repository/     # Spring Data JPA repositories
│       │   │   ├── entity/         # JPA Entities (User, Project, Sprint, Issue, IssueAuditLog)
│       │   │   ├── event/          # Spring ApplicationEvents for async audit logging
│       │   │   └── exception/      # Custom exceptions & GlobalExceptionHandler (@RestControllerAdvice)
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1__init_schema.sql
│       └── test/java/com/tracker/api/
│           ├── service/            # Unit tests for state machine & sprint rollover
│           └── integration/        # Full API integration tests with Testcontainers
└── tracker-cli/
    ├── pom.xml
    └── src/main/java/com/tracker/cli/
        ├── TrackerCliApplication.java
        ├── client/                 # ApiClient wrapper around java.net.http.HttpClient
        ├── config/                 # CredentialStore (~/.tracker/credentials.json)
        └── command/                # Picocli commands (AuthCommand, ProjectCommand, SprintCommand, IssueCommand)
```

---

## Database Schema (`V1__init_schema.sql`)

### 1. `users`
* `id` UUID PRIMARY KEY
* `username` VARCHAR(50) UNIQUE NOT NULL
* `email` VARCHAR(255) UNIQUE NOT NULL
* `password_hash` VARCHAR(255) NOT NULL
* `role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER' (`ADMIN`, `MEMBER`)
* `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

### 2. `projects`
* `id` UUID PRIMARY KEY
* `project_key` VARCHAR(10) UNIQUE NOT NULL (uppercase alphanumeric, e.g., `PROJ`)
* `name` VARCHAR(100) NOT NULL
* `description` TEXT
* `issue_sequence` INT NOT NULL DEFAULT 0 (tracks atomic sequential issue numbering per project)
* `owner_id` UUID NOT NULL REFERENCES `users(id)`
* `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

### 3. `sprints`
* `id` UUID PRIMARY KEY
* `project_id` UUID NOT NULL REFERENCES `projects(id)` ON DELETE CASCADE
* `name` VARCHAR(100) NOT NULL
* `goal` TEXT
* `start_date` DATE
* `end_date` DATE
* `status` VARCHAR(20) NOT NULL DEFAULT 'PLANNED' (`PLANNED`, `ACTIVE`, `CLOSED`)
* `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
* **Constraint:** Partial unique index ensuring at most one `ACTIVE` sprint per project:
  `CREATE UNIQUE INDEX idx_one_active_sprint_per_project ON sprints (project_id) WHERE status = 'ACTIVE';`

### 4. `issues`
* `id` UUID PRIMARY KEY
* `issue_key` VARCHAR(20) UNIQUE NOT NULL (e.g., `PROJ-1`, `PROJ-2`)
* `project_id` UUID NOT NULL REFERENCES `projects(id)` ON DELETE CASCADE
* `sprint_id` UUID REFERENCES `sprints(id)` ON DELETE SET NULL (NULL means issue is in the Backlog)
* `assignee_id` UUID REFERENCES `users(id)` ON DELETE SET NULL
* `reporter_id` UUID NOT NULL REFERENCES `users(id)`
* `title` VARCHAR(255) NOT NULL
* `description` TEXT
* `status` VARCHAR(20) NOT NULL DEFAULT 'BACKLOG' (`BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`)
* `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
* `story_points` INT CHECK (`story_points` >= 0)
* `version` BIGINT NOT NULL DEFAULT 0 (JPA `@Version` for optimistic locking)
* `created_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
* `updated_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

### 5. `issue_audit_logs`
* `id` UUID PRIMARY KEY
* `issue_id` UUID NOT NULL REFERENCES `issues(id)` ON DELETE CASCADE
* `changed_by` UUID NOT NULL REFERENCES `users(id)`
* `field_name` VARCHAR(50) NOT NULL (`STATUS`, `ASSIGNEE`, `PRIORITY`, `SPRINT`, `STORY_POINTS`)
* `old_value` VARCHAR(255)
* `new_value` VARCHAR(255)
* `changed_at` TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

---

## Core Domain Rules & Business Logic

### 1. Issue Key Generation (`ProjectService` / `IssueService`)
* When creating an issue under a project, atomically increment `projects.issue_sequence` (using pessimistic locking `@Lock(LockModeType.PESSIMISTIC_WRITE)` or atomic SQL update) and format the key as `{project_key}-{sequence}` (e.g., `PROJ-14`).

### 2. Issue Status State Machine (`IssueService`)
Enforce valid status transitions and throw an `InvalidStateTransitionException` (HTTP 400) on invalid moves:
* `BACKLOG` -> `TODO`
* `TODO` -> `IN_PROGRESS`, `BACKLOG`
* `IN_PROGRESS` -> `IN_REVIEW`, `TODO`, `DONE`
* `IN_REVIEW` -> `DONE`, `IN_PROGRESS`
* `DONE` -> `IN_PROGRESS` (reopen)
* *Rule:* An issue in `BACKLOG` cannot be moved directly to `DONE`.
* *Rule:* Moving an issue into `TODO`, `IN_PROGRESS`, or `IN_REVIEW` while `sprint_id` is NULL should either require assigning a sprint or automatically attach it to the project's `ACTIVE` sprint if one exists.

### 3. Sprint Lifecycle & Rollover (`SprintService`)
* **Start Sprint (`POST /api/sprints/{sprintId}/start`):**
  * Verify sprint is currently `PLANNED`.
  * Verify no other sprint in the same project has status `ACTIVE` (enforce in service layer + DB partial unique index).
  * Set `status = ACTIVE` and default `start_date = LocalDate.now()` if null.
* **Close Sprint (`POST /api/sprints/{sprintId}/close`):**
  * Execute inside a single `@Transactional` block.
  * Verify sprint is currently `ACTIVE`.
  * Set `status = CLOSED` and `end_date = LocalDate.now()`.
  * **Rollover Logic:** Query all issues in this sprint where `status != DONE`. Accept an optional `targetSprintId` parameter:
    * If `targetSprintId` is provided (must be a `PLANNED` sprint in the same project), reassign unfinished issues to `targetSprintId`.
    * Otherwise, move unfinished issues back to the backlog (`sprint_id = NULL`, and set status from `TODO` back to `BACKLOG` if applicable).

### 4. Optimistic Locking & Audit Trail (`IssueService` & `AuditService`)
* Annotate `Issue.version` with `@Version` to catch concurrent modifications (`ObjectOptimisticLockingFailureException` mapped to HTTP 409 Conflict).
* Publish an `IssueUpdatedEvent` via Spring's `ApplicationEventPublisher` whenever `status`, `assignee_id`, `priority`, `sprint_id`, or `story_points` changes.
* Handle `IssueUpdatedEvent` via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to persist records in `issue_audit_logs`.

### 5. Sprint Metrics & Velocity Endpoint
* `GET /api/projects/{projectKey}/velocity`: Returns completed story points vs. committed story points across all `CLOSED` sprints for the project.
* `GET /api/sprints/{sprintId}/summary`: Returns issue counts grouped by `IssueStatus`, total story points, and completed story points.

---

## REST API Endpoints Summary

### Auth
* `POST /api/auth/register` — Create user account
* `POST /api/auth/login` — Authenticate and return JWT bearer token

### Projects
* `POST /api/projects` — Create a new project
* `GET /api/projects` — List projects
* `GET /api/projects/{projectKey}` — Get project details & backlog count
* `GET /api/projects/{projectKey}/velocity` — Get historical sprint velocity

### Sprints
* `POST /api/projects/{projectKey}/sprints` — Create a `PLANNED` sprint
* `GET /api/projects/{projectKey}/sprints` — List sprints for a project
* `GET /api/projects/{projectKey}/sprints/active` — Get current `ACTIVE` sprint with its issues
* `POST /api/sprints/{sprintId}/start` — Start a planned sprint
* `POST /api/sprints/{sprintId}/close` — Close active sprint & rollover incomplete issues
* `GET /api/sprints/{sprintId}/summary` — Get sprint status breakdown & points

### Issues
* `POST /api/projects/{projectKey}/issues` — Create issue (auto-generates `issue_key`)
* `GET /api/projects/{projectKey}/issues` — Search/filter issues (`?status=`, `?sprintId=`, `?assignee=`)
* `GET /api/issues/{issueKey}` — Get issue details
* `PATCH /api/issues/{issueKey}` — Update issue fields (title, description, priority, assignee, points, sprint)
* `PATCH /api/issues/{issueKey}/status` — Transition issue status (validated by state machine)
* `GET /api/issues/{issueKey}/history` — Retrieve chronological `issue_audit_logs`

---

## CLI Commands Specification (`tracker-cli`)

Configure Picocli root command `tracker` with local JWT persistence in `~/.tracker/credentials.json` (plus configurable server URL defaulting to `http://localhost:8080`).

* `tracker auth login --username <user> --password <pass>`
* `tracker project create --key PROJ --name "Core Platform"`
* `tracker project list`
* `tracker sprint create --project PROJ --name "Sprint 1" --goal "MVP Auth"`
* `tracker sprint start <sprintId>`
* `tracker sprint close <sprintId> [--rollover-to <nextSprintId>]`
* `tracker sprint active --project PROJ` *(renders formatted ASCII table of active sprint issues)*
* `tracker issue create --project PROJ --title "..." [--priority HIGH] [--points 3]`
* `tracker issue list --project PROJ [--status IN_PROGRESS]`
* `tracker issue move <issueKey> --status <NEW_STATUS>`
* `tracker issue assign <issueKey> --user <username>`
* `tracker issue history <issueKey>`

---

## Agent Implementation Instructions (Execution Order)

Execute the implementation in the following incremental milestones, verifying compilation and tests at each step:

1. **Milestone 1 — Project Scaffolding & Docker:**
   * Initialize multi-module `pom.xml` (`tracker-common`, `tracker-api`, `tracker-cli`) targeting Java 21 and Spring Boot 3.4+.
   * Create `docker-compose.yml` with a `postgres:16-alpine` service and healthcheck.
2. **Milestone 2 — Shared Contracts & Database Schema:**
   * Implement DTO records and enums in `tracker-common`.
   * Write `V1__init_schema.sql` in `tracker-api/src/main/resources/db/migration/` and create corresponding JPA entities and repositories.
3. **Milestone 3 — Core Services & Business Rules:**
   * Implement `ProjectService`, `SprintService` (including single-active-sprint validation and transactional rollover), and `IssueService` (atomic issue key sequence, status state machine, optimistic locking, and `IssueUpdatedEvent` publishing).
   * Add `GlobalExceptionHandler` returning RFC 7807 `ProblemDetail` responses for validation, state transition, conflict (409), and not-found (404) errors.
4. **Milestone 4 — REST Controllers, Security & OpenAPI:**
   * Configure stateless JWT authentication in `SecurityConfig` (permitting `/api/auth/**` and `/swagger-ui/**`).
   * Implement REST controllers and configure Springdoc OpenAPI.
5. **Milestone 5 — Integration Testing with Testcontainers:**
   * Write `@SpringBootTest` integration tests backed by Testcontainers PostgreSQL covering:
     * End-to-end auth -> project -> sprint -> issue workflow.
     * Rejection of starting a second active sprint in the same project.
     * Invalid issue status transitions (e.g., `BACKLOG` -> `DONE`).
     * Sprint close rollover moving unfinished issues to the target sprint or backlog.
6. **Milestone 6 — Picocli CLI Client:**
   * Implement `tracker-cli` using Picocli and `java.net.http.HttpClient` with Jackson serialization, local token storage, and clean ASCII table formatting for terminal output.
