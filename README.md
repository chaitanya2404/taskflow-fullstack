# TaskFlow

TaskFlow is a full-stack task/project tracker: a Spring Boot REST API backed by JPA, and a React + TypeScript single-page app that consumes it. Accounts sign in with a JWT; a `User` owns many `Project`s, and a `Project` has many `Task`s, each with a title, description, status, priority, and due date.

Built as a portfolio piece to demonstrate a complete, working full-stack Java application — layered backend architecture, stateless JWT authentication with per-account data isolation, Flyway-managed schema migrations, validated DTOs, global exception handling, OpenAPI docs, a real test suite (unit + integration), a typed React frontend with proper loading/error states, and a containerized deployment.

## Tech stack

**Backend** (`backend/`)
- Java 25 (LTS), Spring Boot 4.1
- Spring Web, Spring Data JPA, Bean Validation
- Spring Security 7 + JWT (JJWT 0.12)
- Flyway (versioned schema migrations)
- H2 (in-memory, `dev` profile) / PostgreSQL (`prod` profile)
- springdoc-openapi (Swagger UI, with bearer-token auth wired in)
- Spring Boot Actuator (health endpoint, used by the container healthcheck)
- JUnit 5, Mockito, Spring `MockMvc`
- Maven

**Frontend** (`frontend/`)
- React 19 + TypeScript
- Vite 8 (build tool/dev server)
- React Router (with an authenticated route guard)
- Plain CSS (no UI framework)

**Infrastructure**
- Multi-stage Dockerfiles for both services
- nginx serving the SPA and reverse-proxying `/api`
- docker-compose: PostgreSQL + backend + frontend

## Architecture

```
┌──────────────────────────┐        HTTP / JSON        ┌───────────────────────────────────┐
│   frontend/  (Vite)      │  ────────────────────▶    │   backend/  (Spring Boot, :8080)   │
│                          │  ◀────────────────────    │                                    │
│  Pages                   │  Authorization:           │  security/     JWT filter, principal│
│  ├─ LoginPage            │    Bearer <jwt>           │  config/       SecurityConfig, JWT  │
│  ├─ ProjectsPage         │                           │  controller/   REST endpoints       │
│  └─ ProjectDetailPage    │      /api/auth            │  service/      business logic       │
│  auth/                   │      /api/projects        │  repository/   Spring Data JPA      │
│  ├─ AuthContext (session)│      /api/tasks           │  entity/       User, Project, Task  │
│  └─ RequireAuth (guard)  │                           │  dto/          request/response +   │
│  api/ (typed fetch client│                           │                validation           │
│       + bearer header)   │                           │  exception/    @RestControllerAdvice│
└──────────────────────────┘                           └───────────────────┬────────────────┘
                                                                            │ Flyway (schema)
                                                                            │ JPA / Hibernate (data)
                                                                            ▼
                                                        ┌────────────────────────────────┐
                                                        │  H2 (dev, in-memory)            │
                                                        │  PostgreSQL (prod / compose)    │
                                                        └────────────────────────────────┘
```

- **Frontend → Backend**: the SPA calls the REST API at `VITE_API_BASE_URL` (defaults to `http://localhost:8080`). Under Docker it is built with an empty base URL, so it calls same-origin `/api/...` and nginx proxies to the backend — no CORS involved.
- **Backend layering**: `controller` (HTTP + validation) → `service` (business rules, transactions, ownership) → `repository` (Spring Data JPA) → `entity` (JPA-mapped `User`/`Project`/`Task`). DTOs (`dto/`) are the only shapes ever exposed over HTTP — entities never leave the service layer.
- **Errors**: a single `@RestControllerAdvice` (`exception/GlobalExceptionHandler`) maps "not found" to 404s, validation failures to 400s with field-level messages, duplicate registrations to 409s, bad credentials to 401s and missing roles to 403s, all using a consistent `ErrorResponse` JSON shape.
- **Schema**: owned by Flyway. JPA runs with `ddl-auto: validate` in every profile, so a mismatch between the entities and the migrations fails the application at startup instead of silently altering tables.

## Authentication and authorization

### The flow

```
POST /api/auth/register  {username, email, password}
        │  password is BCrypt-hashed, account is created with role USER
        ▼
    201 + {token, tokenType, expiresInSeconds, userId, username, email, role}

POST /api/auth/login     {username, password}
        │  credentials checked via AuthenticationManager → DaoAuthenticationProvider
        ▼
    200 + {token, ...}          (401 for both unknown users and wrong passwords)

GET /api/projects
    Authorization: Bearer <token>
        │  JwtAuthenticationFilter verifies the signature, issuer and expiry,
        │  re-loads the account from the database, populates the SecurityContext
        ▼
    200 + only that user's projects        (401 without a valid token)
```

`JwtAuthenticationFilter` re-reads the account on every request rather than trusting the claims wholesale, so a deleted account or a changed role takes effect immediately instead of when the token happens to expire.

Tokens are stateless HS256 JWTs. Nothing is stored server-side, which means **logout is a client-side token discard** — a token stays technically valid until it expires. A token denylist or short-lived tokens plus refresh tokens would be the next step for a real deployment.

### Data ownership

Every project belongs to exactly one user (`projects.owner_id`), and tasks inherit ownership through their project. This is enforced in the query, not by a check after the fact: the repositories only expose owner-scoped finders such as `findByIdAndOwnerId` and `findByIdAndProjectOwnerId`, so there is no code path that can load another user's row and forget to check it.

Cross-account access returns **404, not 403** — a 403 would confirm that the id exists and belongs to somebody, which is a small information leak. `ProjectOwnershipIntegrationTest` proves the guarantee end to end with two real accounts and real tokens, covering reads, updates, deletes, task creation, filtering, and task re-parenting.

### Roles

`USER` and `ADMIN`. Registration always creates a `USER` — a role is never read from the request body, which would let anyone self-promote.

`DELETE /api/projects/{id}` is annotated `@PreAuthorize("hasRole('ADMIN')")` as the project's role-based-authorization example. Note that the role gets a caller past the authorization check but not past ownership: an ADMIN still cannot delete a project owned by someone else (it 404s). The frontend hides the delete button for non-admins rather than showing it and letting the call fail.

To get an ADMIN account, enable the startup seeder (see configuration below). In the `dev` profile it is on by default and creates `admin` / `admin12345` — development-only credentials.

### The JWT secret

The signing key comes from configuration, never from a constant in Java:

| Property | Env var | Dev default |
|---|---|---|
| `taskflow.jwt.secret` | `JWT_SECRET` | a clearly-labelled, publicly-known dev placeholder |
| `taskflow.jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` | `120` |

The dev default exists only so `mvn spring-boot:run` and `mvn test` work with zero setup. It is a known value and must be overridden anywhere real — `docker-compose.yml` deliberately gives `JWT_SECRET` no default, so compose refuses to start until you provide one. The secret must be at least 32 characters (256 bits for HS256); `JwtProperties` validates this at startup so a too-short key fails with a readable configuration error rather than a crypto exception.

## Database migrations (Flyway)

The schema lives in `backend/src/main/resources/db/migration`:

| Migration | What it does |
|---|---|
| `V1__initial_schema.sql` | `projects` and `tasks`, their FK, and indexes on `tasks.project_id` / `tasks.status` |
| `V2__add_users_and_project_ownership.sql` | `users` (unique username + email), `projects.owner_id` with its FK and index |

**One migration set serves both engines.** No `{vendor}`-specific paths were needed, because the SQL sticks to constructs H2 2.x and PostgreSQL 14+ both implement:

- `BIGINT GENERATED BY DEFAULT AS IDENTITY` — the SQL-standard identity syntax, instead of Postgres-only `SERIAL` or H2-only `AUTO_INCREMENT`.
- `TIMESTAMP(6) WITH TIME ZONE` — what Hibernate maps `java.time.Instant` to on both dialects. This matters specifically because `ddl-auto: validate` compares column types, so a "close enough" type would fail the boot.
- `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL` — same spelling on both.

Verified both ways: `mvn test` boots the whole app against H2 with `validate`, and `docker compose up` runs the identical migrations against PostgreSQL 17. If a future migration does need dialect-specific SQL, the clean fix is to set `spring.flyway.locations` to `classpath:db/migration/common,classpath:db/migration/{vendor}` and split only the files that differ.

`V2` adds `owner_id` as nullable, then tightens it to `NOT NULL` — the shape this migration would take against a database that already had projects in it (add column, backfill owners, add the constraint). TaskFlow has never been deployed with data so the backfill is a no-op, and against a populated table the `SET NOT NULL` would fail loudly rather than silently inventing an owner.

## Project structure

```
taskflow-fullstack/
├── backend/
│   ├── Dockerfile              multi-stage: Maven/JDK 25 build → JRE 25 runtime, non-root
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/taskflow/backend/
│       │   ├── TaskflowBackendApplication.java
│       │   ├── config/       SecurityConfig, JwtProperties, OpenApiConfig, AdminAccountSeeder
│       │   ├── security/     JwtService, JwtAuthenticationFilter, AuthenticatedUser,
│       │   │                 AppUserDetailsService, 401/403 JSON handlers
│       │   ├── controller/   AuthController, ProjectController, TaskController
│       │   ├── service/      AuthService, ProjectService, TaskService
│       │   ├── repository/   UserRepository, ProjectRepository, TaskRepository
│       │   ├── entity/       User, Role, Project, Task, TaskStatus, TaskPriority
│       │   ├── dto/          *Request / *Response DTOs, ErrorResponse
│       │   └── exception/    ResourceNotFoundException, DuplicateResourceException,
│       │                     GlobalExceptionHandler
│       ├── main/resources/
│       │   ├── application.yml          (dev + prod profiles)
│       │   └── db/migration/            Flyway migrations
│       └── test/java/com/taskflow/backend/
│           ├── security/     JwtServiceTest
│           ├── service/      ProjectServiceTest, TaskServiceTest (JUnit 5 + Mockito)
│           └── controller/   TaskControllerIntegrationTest, AuthControllerIntegrationTest,
│                             ProjectOwnershipIntegrationTest (@SpringBootTest + MockMvc)
├── frontend/
│   ├── Dockerfile              multi-stage: npm ci + vite build → nginx
│   ├── nginx.conf.template     SPA fallback + /api reverse proxy
│   ├── .dockerignore
│   └── src/
│       ├── api/         typed fetch client (attaches the bearer token) + Auth/Project/Task modules
│       ├── auth/        AuthProvider + context, useAuth, RequireAuth guard, session storage
│       ├── components/  Navbar, Modal, ProjectForm, TaskForm, Badges, LoadingSpinner, ErrorBanner
│       └── pages/       LoginPage, ProjectsPage, ProjectDetailPage
├── docker-compose.yml          postgres + backend + frontend
├── .env.example
├── LICENSE
├── .gitignore
└── README.md
```

## Getting started

### Option A — Docker (full stack, prod-like)

Requires Docker with Compose v2.

```bash
cp .env.example .env       # then set POSTGRES_PASSWORD and JWT_SECRET
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend (nginx) | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

The backend runs the `prod` profile against PostgreSQL. Compose waits on a `pg_isready` healthcheck before starting the backend — Postgres accepts TCP connections well before it can serve queries, so waiting on the port alone would still race Flyway. Postgres data lives in the named volume `taskflow-postgres-data` and survives `docker compose down`; use `down -v` to discard it.

`POSTGRES_PASSWORD` and `JWT_SECRET` are required and have no defaults, so compose fails fast rather than booting with a secret published in this repository. Generate a signing key with `openssl rand -base64 48`.

To get an ADMIN account in the compose setup, set `ADMIN_SEED_ENABLED=true` and `ADMIN_PASSWORD=...` in `.env`.

### Option B — run the services directly

**Prerequisites:** Java 25+ and Maven (`brew install openjdk@25 maven`), Node.js 18+ and npm.

**Backend**

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080` with the `dev` profile: H2 in-memory, Flyway-created schema, a seeded `admin` / `admin12345` account, and a dev JWT secret — no configuration needed.

- **http://localhost:8080/swagger-ui.html** — interactive API docs. Click **Authorize**, paste the `token` from `POST /api/auth/login`, and every subsequent "Try it out" call carries the `Authorization` header.
- **http://localhost:8080/v3/api-docs** — raw OpenAPI JSON
- **http://localhost:8080/h2-console** — H2 web console, dev profile only (JDBC URL `jdbc:h2:mem:taskflow`, user `sa`, empty password)
- **http://localhost:8080/actuator/health** — health check

Run the test suite:

```bash
cd backend
mvn test
```

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

Dev server at `http://localhost:5173`, talking to the backend at `http://localhost:8080`. Copy `.env.example` to `.env` and set `VITE_API_BASE_URL` to point elsewhere. The backend allows `http://localhost:5173` and `http://localhost:4173` as CORS origins by default (`CORS_ALLOWED_ORIGINS` to change).

Build for production:

```bash
npm run build   # type-checks with tsc, then bundles with Vite into frontend/dist/
```

## REST API summary

`/api/auth/**`, the Swagger UI, the OpenAPI JSON, `/actuator/health` and (in dev) the H2 console are public. **Everything else requires `Authorization: Bearer <token>`** and only ever operates on the caller's own data.

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | — | Create a USER account, returns a JWT. `409` if the username or email is taken |
| POST | `/api/auth/login` | — | Exchange credentials for a JWT. `401` for unknown users and wrong passwords alike |
| GET | `/api/auth/me` | Bearer | The account behind the supplied token |

### Projects

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/projects` | Bearer | List *your* projects |
| GET | `/api/projects/{id}` | Bearer | Get one of your projects (`404` if it is not yours) |
| POST | `/api/projects` | Bearer | Create a project owned by you |
| PUT | `/api/projects/{id}` | Bearer | Update one of your projects |
| DELETE | `/api/projects/{id}` | Bearer + **ADMIN** | Delete a project and its tasks. `403` without the role, `404` if not yours |

### Tasks

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/tasks` | Bearer | List your tasks (filter with `?projectId=` and/or `?status=TODO\|IN_PROGRESS\|DONE`) |
| GET | `/api/tasks/{id}` | Bearer | Get one of your tasks |
| POST | `/api/tasks` | Bearer | Create a task in one of your projects |
| PUT | `/api/tasks/{id}` | Bearer | Update one of your tasks (re-parenting is limited to projects you own) |
| DELETE | `/api/tasks/{id}` | Bearer | Delete one of your tasks |

Validation errors return `400` with a `fieldErrors` array (field + message); missing or non-owned resources return `404`. See `GlobalExceptionHandler` for the exact response shape.

### Example

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@example.com","password":"s3cret-password"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -s http://localhost:8080/api/projects -H "Authorization: Bearer $TOKEN"   # 200 []
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/projects    # 401
```

## Frontend behaviour

- Unauthenticated visitors are redirected to `/login` by the `RequireAuth` route guard, which remembers the requested path and returns them there after signing in.
- The session (token + account) is kept in `localStorage`, so a reload keeps you logged in. The trade-off is that a successful XSS could read the token; the more hardened alternative is an httpOnly `SameSite` cookie issued by the backend, which requires CSRF protection in exchange.
- The API client attaches `Authorization: Bearer <token>` to every request. Any `401` from a protected endpoint (typically an expired token) clears the session, which makes the guard bounce the user back to `/login` — login and register responses are excluded so a bad password is reported in the form instead.
- The navbar shows the signed-in username, an `ADMIN` badge where applicable, and a log-out button.

## Deployment notes

The compose setup is a local "prod-like" environment, not a production deployment. Moving it further:

1. **Images**: both Dockerfiles are multi-stage. The backend builds with `maven:3.9-eclipse-temurin-25` and ships only the jar on `eclipse-temurin:25-jre`, running as an unprivileged `taskflow` user — no JDK, no Maven, no sources in the runtime layer. The frontend builds with `node:22-alpine` and ships only `dist/` on `nginx:1.27-alpine` (~22 MB).
2. **Secrets**: `JWT_SECRET` and `POSTGRES_PASSWORD` come from the environment and would move to a secrets manager (AWS Secrets Manager, Kubernetes Secrets, …). `.gitignore` excludes `.env*` except `.env.example`.
3. **Database**: swap the Postgres container for managed Postgres (RDS, Cloud SQL). Flyway runs on backend startup; for multi-replica deployments run migrations as a separate init job or pre-deploy step so replicas do not race, and Flyway's locking is relied on rather than assumed.
4. **TLS and hosting**: terminate TLS at a load balancer or ingress in front of nginx. This maps cleanly onto ECS/Fargate, a Kubernetes Deployment + Service + Ingress, or a PaaS like Render/Railway/Fly.io.
5. **Frontend API URL**: `VITE_API_BASE_URL` is inlined at build time. The compose build leaves it empty so the SPA is same-origin behind nginx; a CDN-hosted frontend talking to a separate API domain would set it at build time and add that origin to `CORS_ALLOWED_ORIGINS`.

## What's simplified (by design, for a portfolio scope)

- **Stateless tokens only** — no refresh tokens and no server-side revocation, so logging out discards the token client-side but does not invalidate it. Short-lived access tokens plus refresh tokens, or a denylist, would be the production answer.
- **No pagination** on list endpoints — fine at demo scale, would add `Pageable` support for production data volumes.
- **No rate limiting** on `/api/auth/login`, which a public deployment would want in front of it (bucket4j, or at the gateway/WAF layer).
- **No sharing model** — a project has exactly one owner, with no way to invite collaborators. Adding that would mean a join table and replacing the `ownerId` checks with a membership lookup.
- **nginx runs as root** in the frontend image (the stock `nginx:alpine` behaviour, workers drop to the `nginx` user). The backend image does run fully non-root.
