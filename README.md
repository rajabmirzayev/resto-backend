# Tabler — Backend

Microservice backend for the Tabler restaurant-management platform. Spring Boot 3 / Java 21, single PostgreSQL database with per-service schemas, Keycloak for auth, a Spring Cloud Gateway entry point, and an optional all-in-one Docker Compose stack.

```
Frontend ──► cloud-gateway (8001) ──► auth-gateway ──► Keycloak (8080)
               │  │  │  └─► 11 business microservices (8102–8113)
               └──────────────► PostgreSQL (5432), Valkey/Redis (6379)
```

- **Auth:** Keycloak 26.5 (`tabler` realm, client `tabler-auth`). The gateway validates JWTs; `auth-gateway` proxies login/refresh/logout. The frontend never talks to Keycloak directly.
- **Database:** one `tabler` database; each service owns a schema (`tabler_user`, `tabler_role`, `tabler_organization`, `tabler_menu`, `tabler_table`, `tabler_order`, `tabler_setting`). Schema migrations live in `db-migrations` (Liquibase).
- **Config:** 100% env-driven via `script/.env`. No secrets in the repository.

---

## Prerequisites

- **Docker** with the Compose plugin (any recent Docker Desktop / docker-ce)
- **Java 21** (only needed for the local-development flow)
- Git

---

## 1. Quick start — everything in Docker (recommended)

This runs the entire backend: database, migrations, Keycloak, both gateways and all 11 services.

```bash
cd script
cp .env.example .env      # then edit .env (passwords, URLs, secrets)
docker compose up -d --build
```

Wait for everything to come up, then run the Keycloak bootstrap **once** to create the platform user:

```bash
docker compose exec keycloak /bin/sh /tmp/scripts/bootstrap-kc.sh
```

Smoke test:

```bash
curl -s http://localhost:8001/actuator/health
curl -s -X POST http://localhost:8001/api/auth-ms/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"platform@codlab.az","password":"<PLATFORM_BOOTSTRAP_PASSWORD>"}'
```

A successful login returns a JWT whose `roles` claim contains `SUPER_ADMIN`.

> **Memory:** the first build compiles 15 modules inside Docker and needs a Docker engine with **at least 6 GB** of memory (Docker Desktop → Settings → Resources). On a low-memory machine the build can fail with `ResourceExhausted: cannot allocate memory`; build one service at a time as a workaround:
>
> ```bash
> docker compose build --parallel user-service role-service organization-service
> ```
> `docker compose up -d --build` takes several minutes the first time. Use `docker compose logs -f <service>` to follow any container.

---

## 2. Local development (services in IDE / Gradle, infra in Docker)

Use this when you want to edit and restart individual services quickly.

### 2.1 Prepare the environment file

```bash
cp .env.example .env
```

The `.env` ships with development-friendly values (`mysecretpassword`, `platform123`, …). Change them if your local Postgres differs. `.env` is git-ignored.

### 2.2 Start infrastructure

```bash
docker compose -f script/local-compose.yml up -d
```

This starts: `db` (Postgres 18 on :5432), `keycloak` (:8080), `valkey` (Redis :6379) and `pgadmin` (:5050). Keycloak imports the realm automatically. Wait until it is healthy:

```bash
docker compose -f script/local-compose.yml ps
# keycloak must show (healthy)
```

### 2.3 Run schema migrations

```bash
set -a && source script/.env && set +a && ./gradlew :db-migrations:bootRun
```

This applies all Liquibase changesets and exits (`Started DbMigrationApplication … exit`).

### 2.4 Bootstrap Keycloak (platform user)

```bash
docker compose -f script/local-compose.yml exec keycloak /bin/sh /tmp/scripts/bootstrap-kc.sh
```

Idempotent — safe to re-run. Creates/updates the `tabler-auth` client secret, creates `PLATFORM_BOOTSTRAP_EMAIL` (default `platform@codlab.az`), sets its password and assigns `SUPER_ADMIN`.

### 2.5 Run the services

Each service is started the same way, for example:

```bash
set -a && source script/.env && set +a && ./gradlew :menu-service:bootRun
```

Services (start gateways first, then the rest; startup order between business services does not matter):

| Module | Port | DB schema |
|---|---|---|
| `cloud-gateway` | 8001 | – |
| `auth-gateway` | 8002 | – |
| `organization-service` | 8102 | `tabler_organization` |
| `user-service` | 8103 | `tabler_user` |
| `role-service` | 8104 | `tabler_role` |
| `menu-service` | 8105 | `tabler_menu` |
| `table-service` | 8106 | `tabler_table` |
| `order-service` | 8107 | `tabler_order` |
| `kitchen-service` | 8108 | – |
| `waiter-service` | 8109 | – |
| `customer-service` | 8110 | – |
| `setting-service` | 8111 | `tabler_setting` |
| `dashboard-service` | 8112 | – |
| `report-service` | 8113 | – |

Services with no schema column (kitchen, waiter, customer, dashboard, report) are stateless aggregators that call other services via Feign.

### 2.6 Verify

```bash
# health of the gateway
curl -s http://localhost:8001/actuator/health

# login through the gateway (expect a JWT)
curl -s -X POST http://localhost:8001/api/auth-ms/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"platform@codlab.az","password":"platform123"}'
```

---

## 3. Deploy to a server

The same `compose.yml` used for the quick start is the deployment unit.

### 3.1 On the server

1. Install **Docker** and the compose plugin.
2. Copy the project and prepare the env:
   ```bash
   git clone <your-repo> tabler-back && cd tabler-back/script
   cp .env.example .env
   ```
3. Set production values in `.env`:
   - `POSTGRES_PASSWORD`, `KC_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `PLATFORM_BOOTSTRAP_PASSWORD`, `AUTH_KEYCLOAK_CLIENT_SECRET`, `PGADMIN_DEFAULT_PASSWORD` → strong random values.
   - `CORS_ALLOWED_ORIGIN_PATTERNS` → your frontend origin(s), e.g. `https://app.example.com`.
   - `MENU_PUBLIC_BASE_URL` → `http://<server-ip-or-domain>:8001` (image URLs are built from it).
4. Start the stack:
   ```bash
   docker compose up -d --build
   ```
5. Bootstrap Keycloak once:
   ```bash
   docker compose exec keycloak /bin/sh /tmp/scripts/bootstrap-kc.sh
   ```

### 3.2 Firewall / networking

Only **8001** (the gateway) must be reachable from outside; Keycloak, Postgres and pgAdmin stay private on the Docker network:

- `8001` — API entry point (frontend talks only to this)
- `5050` — pgAdmin (optional; restrict by IP or VPN)
- `5432`, `8080`, `6379`, `8102–8113` — internal only

Put an HTTPS reverse proxy (nginx, Caddy, Traefik) in front of `:8001` for production TLS.

> **Note:** the Keycloak container currently runs `start-dev` (HTTP, dev profile). For a production deployment, run Keycloak behind TLS and enable its production mode (`kc.sh start` + `KC_HOSTNAME`, HTTPS), then swap `start-dev` in `compose.yml`.

### 3.3 Update / maintenance

```bash
git pull && cd script
docker compose up -d --build --remove-orphans   # rebuilds changed images, leaves data volumes intact
docker compose exec keycloak /bin/sh /tmp/scripts/bootstrap-kc.sh   # no-op if nothing changed
```

Backups: `docker compose exec db pg_dump -U postgres tabler > backup.sql`.

---

## 4. Configuration reference

All runtime configuration lives in `script/.env` (see `script/.env.example` for the template with `change-me` placeholders). Never commit `.env`.

| Variable | Used by | Purpose |
|---|---|---|
| `POSTGRES_USER/DB/PASSWORD` | db, migrations, all services | Postgres credentials |
| `POSTGRES_URL` | `db-migrations` | JDBC URL (local: `localhost`, docker: `db`) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | gateways | Allowed frontend origins |
| `KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD` | keycloak | Keycloak admin account |
| `AUTH_KEYCLOAK_CLIENT_SECRET` | auth-gateway, bootstrap | Secret of the `tabler-auth` client |
| `PLATFORM_BOOTSTRAP_EMAIL/PASSWORD/ORG_ID` | bootstrap | Initial platform super-admin user |
| `MENU_PUBLIC_BASE_URL` | menu-service | Base URL for menu image URLs |
| `MENU_STORAGE_DIR` | menu-service | Where menu images are stored |
| `GW_*` | cloud-gateway | Route target URLs, Keycloak issuer, Redis |
| `AUTH_KEYCLOAK_URL` | auth-gateway | Keycloak base URL |
| `<SERVICE>_DB_URL` | each DB service | JDBC URL including `?currentSchema=…` |
| `JPA_DDL_AUTO` | all DB services | Hibernate DDL mode (`validate` recommended) |

`local-compose.yml` and `compose.yml` reference these variables directly — `docker compose` substitutes them from `.env` automatically.

---

## 5. Project layout

```
common-core/                  shared enums and value types (no JPA)
common-jpa/                   shared JPA entities/auditing (depends on common-core)
common-security/              shared security helpers
common-exception-handling/    shared error handling / API errors
db-migrations/                Liquibase changelogs (all schemas), one-shot runner
auth-gateway/                 login/refresh/logout proxy to Keycloak
cloud-gateway/                Spring Cloud Gateway: routing, JWT validation, CORS, rate limiting
organization|user|role|menu|table|order|setting-service/   DB-backed services
kitchen|waiter|customer|dashboard|report-service/          stateless aggregators (Feign)
script/
  .env / .env.example         environment (git-ignored / template)
  compose.yml                 full stack (server / quick start)
  local-compose.yml           infra only (local development)
  tabler-realm.json           Keycloak realm export
  bootstrap-kc.sh             creates the platform user + client secret (idempotent)
```

---

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| `keycloak` never shows `healthy` | Health is on the management port `:9000` inside the container; the compose healthcheck uses `bash /dev/tcp`. Check `docker compose logs keycloak`. |
| `Imported realm` but login fails with invalid credentials | Run the bootstrap step; the platform user is not created by the realm import. |
| Services restart in a loop right after `up` | Wait for `migrations` to finish (`service_completed_successfully`); check `docker compose logs migrations`. |
| `Failed to determine a suitable driver class` | Happens if a stateless service got `common-jpa` back on its classpath — those use `common-core` instead. Rebuild with `--build`. |
| Port already in use on `5432/8080/8001/…` | Stop the local service/container or change the port mapping in the compose file / `.env`. |
| Login works locally but not after deploy | `AUTH_KEYCLOAK_CLIENT_SECRET` must match the value written into Keycloak by bootstrap (or re-run bootstrap). |
