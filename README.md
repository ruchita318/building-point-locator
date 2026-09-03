# Building Point Locator — Angular + Spring Boot + PostgreSQL/PostGIS

Full-stack 3D building/floor point locator with Angular 20, Java 21/Spring Boot 3.5, PostgreSQL/PostGIS, Flyway and Spring Security. The seeded data contains 18 buildings with square and octagonal footprints.

## Run with Docker

```bash
docker compose up --build
```
Open `http://localhost:8080`. The Compose demo uses HTTP Basic credentials `demo-api-user` / `demo-api-password`. Replace these for any real deployment.

## Run locally without Docker

Prerequisites: Java 21, Maven 3.9+, Node.js 20+, npm, PostgreSQL 16 and PostGIS.

Create the database:

```sql
CREATE USER locator WITH PASSWORD 'locator_local_password';
CREATE DATABASE building_locator OWNER locator;
\c building_locator
CREATE EXTENSION IF NOT EXISTS postgis;
```

Start backend service:

```bash
cd building-point-locator-service
mvn spring-boot:run
```

`local` is the default profile. You can also run explicitly:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway creates/seeds the schema automatically.

Start frontend in a second terminal:

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200`. `proxy.conf.json` forwards `/api` to `http://localhost:8080`.

## Spring profiles

- `application.yml`: shared server/Actuator/CORS config; defaults to `local`.
- `application-local.yml`: localhost DB defaults and security disabled for development.
- `application-prod.yml`: production settings; requires DB/API credentials from environment variables.

Production example:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://prod-db:5432/building_locator
export DB_USERNAME=locator_service
export DB_PASSWORD='from-secret-manager'
export API_USERNAME=locator-api
export API_PASSWORD='from-secret-manager'
java -jar target/building-point-locator-1.0.0.jar
```

Never commit real production passwords. Use a secret manager (Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault, Kubernetes External Secrets, etc.).

## Security

Local security is disabled by default. Enable it locally with:

```bash
export SECURITY_ENABLED=true
export API_USERNAME=test-user
export API_PASSWORD=test-password
```

Production enables stateless Spring Security HTTP Basic for `/api/**` by default. Health endpoints and frontend static files stay public. HTTP Basic is deliberately simple for this assignment; for a public system use OAuth2/OIDC with short-lived tokens and an identity provider.

## API

Health checks:

- `GET /api/health`: aggregate application health, returns `200 OK` when live and ready or `503 Service Unavailable`
- `GET /api/health/live`: liveness probe, returns `200 OK` when the process is alive or `503 Service Unavailable`
- `GET /api/health/ready`: readiness probe, returns `200 OK` when the service can accept traffic or `503 Service Unavailable`
- Spring Boot Actuator health remains available at `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`

`POST /api/locate`

```json
{"x":15,"y":15,"z":1}
```

Response:

```json
{"found":true,"building":"Office building","floor":"Floor 1","message":"Point is inside Office building, Floor 1."}
```

PostGIS uses `ST_Covers`, so outline boundaries count as inside. Z uses lower-inclusive/upper-exclusive floor ranges to prevent overlap at shared floor boundaries.

## Backend tests

```bash
cd building-point-locator-service
mvn test
```

The default suite includes unit and component tests only. These cover service match/no-match behavior, controller success, missing/null coordinates, response construction and API wiring through Spring MVC and Spring Security with a mocked repository. They do not require Docker or a database.

Run DB-backed integration tests with Docker running:

```bash
cd building-point-locator-service
mvn verify -Pintegration-test
```

The integration profile uses `src/test/resources/application-integration-test.yml` and Testcontainers to start a real PostGIS database, run Flyway migrations, seed data and verify `/api/locate` through the controller, service, repository and spatial SQL.

## Frontend tests

```bash
cd frontend
npm install
npm test
```

Watch mode:

```bash
npm run test:watch
```

Included Angular tests cover rendering, success/not-found/error states and the exact `POST /api/locate` service request. The test target enables code coverage.

## CI/CD pipeline

GitHub Actions runs `.github/workflows/ci-cd.yml` on pull requests, pushes to `main` and manual dispatches. The pipeline stages are:

1. Checkstyle and lint: `mvn checkstyle:check` for the backend and `npm run lint` for the frontend.
2. Unit tests: backend tests excluding `*ComponentTest` and `*IntegrationTest`, plus `npm run test:unit`.
3. Component tests: backend `*ComponentTest` classes.
4. Integration tests: backend `*IntegrationTest` classes through the Maven `integration-test` profile, plus `npm run test:integration`.
5. Docker image build: builds the root `Dockerfile` as `building-point-locator:${GITHUB_SHA}`.
6. Fake Argo CD deployment: echoes the `argocd app set`, `argocd app sync` and `argocd app wait` commands without changing any cluster resources.

## Build without Docker

Backend:

```bash
cd building-point-locator-service
mvn clean package
java -jar target/building-point-locator-1.0.0.jar
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

## Configuration variables

| Variable | Local default | Production |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | `prod` |
| `DB_URL` | localhost DB | required |
| `DB_USERNAME` | `locator` | required |
| `DB_PASSWORD` | local-only password | required secret |
| `DB_POOL_MAX_SIZE` | 10 | 20 |
| `DB_POOL_MIN_IDLE` | 2 | 5 |
| `SECURITY_ENABLED` | false | true |
| `API_USERNAME` | local-user | required |
| `API_PASSWORD` | local-password | required secret |
| `CORS_ALLOWED_ORIGINS` | localhost:4200 | production frontend origin |
| `SERVER_PORT` | 8080 | 8080 |
| `LOG_PATH` | `logs` | log directory |
| `LOG_FILE` | `logs/building-point-locator.log` | active application log file |

See `PRODUCTION.md` for scalability, performance, observability, HA, security and CI/CD recommendations.
