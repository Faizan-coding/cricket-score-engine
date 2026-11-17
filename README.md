# Real-Time Cricket Score Engine

A Java / Spring Boot backend that ingests ball-by-ball events for a **two-innings match**, derives the live score, sets the chase target, declares the winner, caches state, and pushes updates to subscribers in real time.

**Stack:** Java 17, Spring Boot 3, Spring Data JPA, MySQL (H2 for local), Redis, Server-Sent Events, Docker, JUnit 5.

## Run it

```bash
# 1. Zero setup (H2 + in-memory cache) with a simulated match playing live
mvn spring-boot:run -Dspring-boot.run.profiles=simulate
# open http://localhost:8080/?match=1

# 2. Score a match yourself (no simulator)
mvn spring-boot:run
# scorer:  http://localhost:8080/scorer.html
# viewer:  http://localhost:8080/?match=1

# 3. Full stack: MySQL + Redis + app + simulator
docker compose up --build

# 4. Tests
mvn test
```

## Match rules

- **Team A bats first**, then Team B chases. The innings changes automatically when the overs are bowled or 10 wickets fall.
- **Target** = first-innings runs + 1, and a required run rate is shown during the chase.
- **Result is declared automatically:** `B won by N wickets` (target reached), `A won by N runs` (chase falls short), or `Match tied`.
- Ball events are rejected once the match is complete. Ties are not decided by a super over, and there is no DLS.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/matches` | Create a match `{teamA, teamB, oversLimit}` |
| POST | `/api/matches/{id}/start` | Move match to LIVE |
| POST | `/api/matches/{id}/events` | Record a ball (idempotent on `eventId`) |
| GET | `/api/matches/{id}` | Current state: innings, score, target, result (cached) |
| GET | `/api/matches/{id}/events` | Ball-by-ball log for both innings |
| GET | `/api/matches/{id}/stream` | Live updates (SSE) |
| GET | `/api/matches/{id}/verify` | Replay the log (both innings) and compare with the stored match |

```bash
curl -X POST localhost:8080/api/matches -H 'Content-Type: application/json' \
  -d '{"teamA":"India","teamB":"Australia","oversLimit":5}'

curl -X POST localhost:8080/api/matches/1/start

curl -X POST localhost:8080/api/matches/1/events -H 'Content-Type: application/json' \
  -d '{"eventId":"e-001","type":"RUNS","runs":4,"batter":"Kohli","bowler":"Starc"}'
```

- **Event types:** `RUNS` (0-6), `WICKET` (0-3 runs), `WIDE` (0-4 extra), `NO_BALL` (0-6 off the bat), `BYE` / `LEG_BYE` (1-4).
- **Responses:** `201` new event, `200` duplicate `eventId`, `409` too much contention (safe to retry), `422` rule violation, `429` rate limited.

## Design decisions

- **Event log + derived state.** Every ball is stored in an append-only `ball_event` table; the match state is derived from it. `/verify` replays both innings and checks the result equals the stored match.
- **Pure rules engine.** `ScoreCalculator` and `MatchEngine` (innings change-over, target, winner) have no framework dependencies and are unit tested on their own.
- **Idempotency.** The client sends a unique `eventId`. A unique constraint on `(match_id, event_id)` guarantees a retried request is never counted twice.
- **Concurrency.** `Match` has an `@Version` column (optimistic locking) and `(match_id, sequence_no)` is unique. Conflicting writers fail and are retried outside the transaction with a short random backoff, so no update is lost or duplicated.
- **Caching.** Reads go cache-first (Redis or in-memory). The cache and SSE push are updated only after the DB transaction commits, and an older state can never overwrite a newer one.
- **Push, not poll.** Subscribers get updates over SSE; the page ignores stale messages by `eventCount`.
- **Rate limiting.** Per-IP fixed-window limiter on writes.

## Known limitations / next steps

- Redis check-then-set is not atomic (a Lua script would fix this).
- SSE subscribers live in one app instance; for multiple instances, fan out via Redis pub/sub.
- Schema is created with `ddl-auto: update`; use Flyway for real migrations.
- Team A always bats first; no toss, super over, or DLS; no authentication on the scorer API.

## Load testing

```bash
k6 run -e MATCH=1 loadtest/read-score.js
```

Measures p95 latency and throughput of the cached read path; compare runs with and without Redis.
