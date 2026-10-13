# Performance results

Load test: `k6/search-and-recommend.js`, 30 seconds, 10 virtual users on `GET /api/jobs` and 5 on
`GET /api/jobs/recommended`, running at the same time. Data: 20,000 generated jobs with 4 skills each
(`docs/performance/seed-jobs.sql`). The app runs with `mvnw spring-boot:run`, PostgreSQL 17 in Docker,
both on the same laptop. The first run warms up the JVM, so the numbers below are from the second run.

## Baseline

| Endpoint | avg | p(95) | max |
|---|---|---|---|
| `GET /api/jobs` (search) | 100 ms | 159 ms | 422 ms |
| `GET /api/jobs/recommended` | 2.00 s | 2.76 s | 2.82 s |

About 91 requests per second in total. The recommendations threshold (p95 under 800 ms) fails.