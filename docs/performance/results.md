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

## After optimization: recommendations

Change: the recommendation query now returns light `(jobId, publishedAt, skillName)` rows instead of
fully loaded `Job` entities. Scoring and ranking happen on those rows, and full entities are
loaded only for the 20 jobs on the requested page.

| Metric | Before | After |
|---|---|---|
| SQL statements per `/recommended` request | 260 | 9 |
| Single request (cold-ish, curl-style) | 1207 ms | 387 ms |
| k6 `recommended` p95 (5 VUs) | 2.76 s | 861 ms |
| k6 `search` p95 (10 VUs) | 159 ms | 147 ms |
| Total throughput | ~91 req/s | ~103 req/s |

Target for `recommended` was p95 < 800 ms. It is not met yet (861 ms).

What is left: the query still reads every job that shares at least one skill with the candidate
and scores them in Java. With 20k seeded jobs that is tens of thousands of rows per request.
The next step would be to push the scoring into SQL or to cache the candidate set.

Environment: everything on one laptop (app, PostgreSQL 17 in Docker, k6 in Docker), so absolute
numbers are only comparable between runs on the same machine.