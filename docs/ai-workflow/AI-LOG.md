# AI Log

Record of real cases where an AI agent was used in this project:
what it produced, how it was verified, and where it was wrong.
Only real cases are logged.

## Entry template

### YYYY-MM-DD - short task title
- **Tool:** Claude (chat) / Claude Code / other
- **Task:** what was asked
- **AI output:** what it generated
- **Verification:** how it was checked (tests, manual review, mutation testing)
- **Result:** accepted / fixed / rejected
- **What was wrong and how it was found:** (if anything)
- **Takeaway:** what changes in the process

## Entries

### 2026-10-13 - Slow recommendations endpoint
- **Tool:** Claude (chat), step-by-step guidance
- **Task:** find out why `GET /api/jobs/recommended` was slow under load and fix it.
- **AI output:** a k6 script, a seed script for 20k jobs, a way to count SQL statements per request, and a rewrite of `RecommendationService` and `JobRepository`.
- **Verification:** measured before and after on the same machine. SQL statements per request via `org.hibernate.SQL` debug logging, latency via k6. The 110 existing tests had to stay green.
- **Result:** fixed, goal only partly met.
- **What was wrong and how it was found:** the service loaded every matching `Job` entity and its skills, which meant 260 SQL statements per request (p95 2.76 s). The first idea for measuring (reading Hibernate session metrics from the logs) produced no output, so I switched to counting SQL log lines. After the change: 9 statements, p95 861 ms. The target of 800 ms is still not met, see `docs/performance/results.md`.
- **Takeaway:** measure first, then change code. A hypothesis about the cause is not a result until the SQL count confirms it.

### 2026-10-12 - Skill matching: duplicate skills with different case
- **Tool:** Claude (chat)
- **Task:** weighted scoring of candidate skills against a job's skills (`WeightedSkillMatchScorer`).
- **AI output:** a scorer that removed duplicate job skills with a `LinkedHashSet`.
- **Verification:** unit tests, including a case where a job lists "Java" and "java".
- **Result:** fixed.
- **What was wrong and how it was found:** a set of raw strings does not treat "Java" and "java" as the same skill, so the job would be counted with a duplicate. The unit test for this case would have failed. The fix keys a `LinkedHashMap` by the normalized skill name.
- **Takeaway:** write the edge-case test for data normalization before trusting the implementation.

### 2026-10-13 - Missing class in a generated set of files
- **Tool:** Claude (chat)
- **Task:** application tracking: service, controller and exception handling.
- **AI output:** services and exception handler that referenced `DuplicateApplicationException`, but the class itself was not among the files I was given.
- **Verification:** `mvnw test` after pasting the files.
- **Result:** fixed.
- **What was wrong and how it was found:** the compiler reported 5 errors (cannot find symbol). Creating the missing file fixed all of them.
- **Takeaway:** compile after every batch of files, not at the end. When a class is referenced in new code, check that it exists.

### 2026-10-12 - Test files in the wrong source folder
- **Tool:** Claude (chat)
- **Task:** add tests for the new feature.
- **AI output:** test classes with correct content.
- **Verification:** `mvnw test`.
- **Result:** fixed.
- **What was wrong and how it was found:** the test files were created under `src/main/java`, where test dependencies are not on the classpath. The build failed with 91 compile errors. Moving the files to `src/test/java` fixed it.
- **Takeaway:** every file in a code block comes with its full path, and test files always go in `src/test/java`.

### 2026-10-12 - Uncertain API in a new framework version
- **Tool:** Claude (chat)
- **Task:** job search with filters using JPA Specifications on Spring Boot 4.
- **AI output:** a version of the Specification code that avoided the exact generic signature, because that signature might have changed in Spring Data 4.
- **Verification:** the project compiled and the search integration tests passed against PostgreSQL in Testcontainers.
- **Result:** accepted.
- **What was wrong and how it was found:** nothing failed, but the AI was not sure about the new API, and the choice was made to rely on the compiler and tests instead of memory of older versions.
- **Takeaway:** on a new major version, treat AI knowledge of the API as unconfirmed until it compiles and a test exercises it.