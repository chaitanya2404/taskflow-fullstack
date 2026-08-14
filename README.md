# TaskFlow

TaskFlow is a full-stack task/project tracker: a Spring Boot REST API backed by JPA, and a React + TypeScript single-page app that consumes it. A `Project` has many `Task`s; each `Task` has a title, description, status, priority, and due date.

Built as a portfolio piece to demonstrate a complete, working full-stack Java application — layered backend architecture, validated DTOs, global exception handling, OpenAPI docs, a real test suite (unit + integration), and a typed React frontend with proper loading/error states.

## Tech stack

**Backend** (`backend/`)
- Java 25 (LTS), Spring Boot 4.1
- Spring Web, Spring Data JPA, Bean Validation
- H2 (in-memory, `dev` profile) / PostgreSQL (`prod` profile, documented)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, Spring `MockMvc`
- Maven

**Frontend** (`frontend/`)
- React 19 + TypeScript
- Vite 8 (build tool/dev server)
- React Router
- Plain CSS (no UI framework)

## Architecture

```
┌─────────────────────────┐        HTTP / JSON        ┌───────────────────────────────────┐
│   frontend/  (Vite)      │  ────────────────────▶   │   backend/  (Spring Boot, :8080)   │
│                          │  ◀────────────────────    │                                    │
│  Pages                   │      /api/projects        │  controller/   REST endpoints      │
│  ├─ ProjectsPage         │      /api/tasks            │  service/      business logic      │
│  └─ ProjectDetailPage    │                            │  repository/   Spring Data JPA     │
│  Components               │                           │  entity/       Project, Task       │
│  ├─ ProjectForm/TaskForm │                            │  dto/          request/response +  │
│  └─ Badges/Modal/...     │                            │                validation          │
│  api/ (typed fetch client)│                           │  exception/    @RestControllerAdvice│
└─────────────────────────┘                            └───────────────────┬────────────────┘
                                                                              │ JPA / Hibernate
                                                                              ▼
                                                          ┌────────────────────────────────┐
                                                          │  H2 (dev, in-memory)            │
                                                          │  PostgreSQL (prod, documented)  │
                                                          └────────────────────────────────┘
```

- **Frontend → Backend**: the SPA calls the REST API at `VITE_API_BASE_URL` (defaults to `http://localhost:8080`).
- **Backend layering**: `controller` (HTTP + validation) → `service` (business rules, transactions) → `repository` (Spring Data JPA) → `entity` (JPA-mapped `Project`/`Task`, one-to-many). DTOs (`dto/`) are the only shapes ever exposed over HTTP — entities never leave the service layer.
- **Errors**: a single `@RestControllerAdvice` (`exception/GlobalExceptionHandler`) turns "not found" into 404s and validation failures into 400s with field-level messages, using a consistent `ErrorResponse` JSON shape.
- **Database**: H2 in-memory for local dev/tests (zero setup, resets on restart). A `prod` Spring profile targeting PostgreSQL is fully configured and documented, but not required to run this project locally.

## Project structure

```
taskflow-fullstack/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/taskflow/backend/
│       │   ├── TaskflowBackendApplication.java
│       │   ├── controller/   ProjectController, TaskController
│       │   ├── service/      ProjectService, TaskService
│       │   ├── repository/   ProjectRepository, TaskRepository
│       │   ├── entity/       Project, Task, TaskStatus, TaskPriority
│       │   ├── dto/          *Request / *Response DTOs, ErrorResponse
│       │   └── exception/    ResourceNotFoundException, GlobalExceptionHandler
│       ├── main/resources/application.yml   (dev + prod profiles)
│       └── test/java/com/taskflow/backend/
│           ├── service/      ProjectServiceTest, TaskServiceTest (JUnit 5 + Mockito)
│           └── controller/   TaskControllerIntegrationTest (@SpringBootTest + MockMvc)
├── frontend/
│   └── src/
│       ├── api/         typed fetch client + Project/Task API modules
│       ├── components/  Navbar, Modal, ProjectForm, TaskForm, Badges, LoadingSpinner, ErrorBanner
│       └── pages/        ProjectsPage, ProjectDetailPage
├── LICENSE
├── .gitignore
└── README.md
```

## Getting started

### Prerequisites
- Java 25+ and Maven (or use the instructions below if you only have Homebrew: `brew install openjdk@25 maven`)
- Node.js 18+ and npm

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080` using the `dev` profile (H2 in-memory database, auto-created schema, no configuration needed). Swagger UI is available at:

- **http://localhost:8080/swagger-ui.html** — interactive API docs
- **http://localhost:8080/v3/api-docs** — raw OpenAPI JSON
- **http://localhost:8080/h2-console** — H2 web console (JDBC URL `jdbc:h2:mem:taskflow`, user `sa`, empty password)

Run the backend test suite (unit tests for the service layer + a full MockMvc integration test hitting real controller endpoints against the real H2 database):

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts at `http://localhost:5173` and talks to the backend at `http://localhost:8080` by default. To point it elsewhere, copy `.env.example` to `.env` and set `VITE_API_BASE_URL`.

Build for production:

```bash
npm run build   # type-checks with tsc, then bundles with Vite into frontend/dist/
```

## REST API summary

| Method | Path | Description |
|---|---|---|
| GET | `/api/projects` | List all projects |
| GET | `/api/projects/{id}` | Get a project |
| POST | `/api/projects` | Create a project |
| PUT | `/api/projects/{id}` | Update a project |
| DELETE | `/api/projects/{id}` | Delete a project (cascades to its tasks) |
| GET | `/api/tasks` | List tasks (optionally filter with `?projectId=` and/or `?status=TODO\|IN_PROGRESS\|DONE`) |
| GET | `/api/tasks/{id}` | Get a task |
| POST | `/api/tasks` | Create a task |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |

Validation errors return `400` with a `fieldErrors` array (field + message); missing resources return `404`. See `GlobalExceptionHandler` for the exact response shape.

## Deployment notes

This project ships configured for zero-setup local development (H2 in-memory), but is structured to move to a containerized production setup:

1. **Database**: run PostgreSQL (e.g. the official `postgres:16` Docker image) and start the backend with `--spring.profiles.active=prod`, supplying `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as environment variables (see the `prod` block in `backend/src/main/resources/application.yml`). Use a real migration tool (Flyway/Liquibase) instead of `ddl-auto` for schema changes in production.
2. **Backend container**: multi-stage Dockerfile — `maven:3.9-eclipse-temurin-25` to build the jar, then copy it into a slim `eclipse-temurin:25-jre` runtime image; expose port 8080.
3. **Frontend container**: `npm run build` produces static assets in `frontend/dist/`; serve them from an `nginx:alpine` image (or a CDN/static host), with `VITE_API_BASE_URL` baked in at build time to point at the deployed backend's public URL.
4. **Orchestration**: a `docker-compose.yml` with three services (`postgres`, `backend`, `frontend`) is a natural next step for local "prod-like" testing; for real deployment, this maps cleanly onto ECS/Fargate, a Kubernetes Deployment + Service, or a PaaS like Render/Railway/Fly.io, with managed Postgres (RDS, Cloud SQL, etc.) instead of a self-hosted container.
5. **Config/secrets**: database credentials and any future auth secrets would move to environment variables / a secrets manager — never committed (see `.gitignore`, which excludes `.env*` except `.env.example`).

## What's simplified (by design, for a portfolio scope)

- No authentication/authorization layer — every endpoint is open. Adding Spring Security + JWT would be the natural next step.
- No pagination on list endpoints — fine at demo scale, would add `Pageable` support for production data volumes.
- No Flyway/Liquibase migrations — `ddl-auto: update` is used for the `dev` H2 profile for simplicity; the `prod` profile intentionally uses `ddl-auto: validate` to make clear that schema management should be externalized in a real deployment.
