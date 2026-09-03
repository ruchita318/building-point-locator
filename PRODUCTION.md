# Production readiness

## Scalability
The Spring Boot API is stateless, so run multiple replicas behind a load balancer/API gateway and scale horizontally (for example Kubernetes Deployment + HPA). Size Hikari pools carefully: total DB connections = replica count × pool size. Use PgBouncer when replica count grows.

## Spatial performance
Keep point-in-polygon work in PostGIS. Maintain GiST spatial indexes, pre-filter by Z, inspect `EXPLAIN (ANALYZE, BUFFERS)`, validate geometry at ingestion, and monitor slow queries. For very large datasets consider precomputed envelopes, partitioning by site/region and geometry simplification where precision permits.

## Security
Use TLS, WAF/rate limiting, least-privilege DB credentials, strict CORS, secure headers, dependency/image scanning and a secret manager. Replace assignment-level HTTP Basic with OAuth2/OIDC and short-lived tokens for a public service.

## Database HA
Prefer managed PostgreSQL/PostGIS with multi-zone failover, automated backups, point-in-time recovery and tested restore procedures. Read replicas help only for read paths designed to use them.

## Observability
Use Micrometer/OpenTelemetry, Prometheus/Grafana and centralized structured logs. The application writes console logs and a rolling file log configurable with `LOG_PATH` and `LOG_FILE`; ship these to your centralized logging platform in production. Track request count, errors, p95/p99 latency, spatial query latency, DB pool saturation, PostgreSQL CPU/IO, JVM heap/GC and container restarts.

## Reliability
Use readiness/liveness probes, graceful shutdown, explicit timeouts, bounded retries and SLO-driven alerting. The service exposes `/api/health`, `/api/health/live`, `/api/health/ready`, and the equivalent Spring Boot Actuator health endpoints. Example targets (to be validated by load tests): 99.9% availability, p95 locate latency under 200 ms, 5xx below 0.1%.

## Caching
Do not add Redis by default. First measure PostGIS with the spatial index. If repeated identical/quantized point lookups are common, consider Caffeine or Redis with a clear invalidation strategy when geometry changes.

## CI/CD
Run backend tests, Angular tests, production builds, static analysis, dependency/SBOM scan, container build/scan, PostGIS Testcontainers integration tests, staging smoke tests and controlled production rollout. Use immutable image tags tied to Git SHA.

## Additional tests before public launch
Add PostGIS/Testcontainers repository tests, Flyway migration tests, Spring Security authorization tests, Playwright/Cypress E2E tests, accessibility tests, load tests and geometry edge cases (polygon edges/vertices, concave shapes, exact Z boundaries, smaller floor footprints, overlaps and malformed geometry).
