# Bet Task

Spring Boot service for the Kafka-based sports betting settlement assignment.

## What It Does

```
HTTP POST /api/event-outcomes
       |
       v
  Kafka topic: event-outcomes
       |
       v
  Kafka consumer
       |
       v
  Bet matching (by Event ID, unsettled only)
       |
       v
  Determine WON / LOST per bet
       |
       v
  RocketMQ producer (mocked, logs payload)
```

1. Accepts an event outcome (event ID, event name, winner ID) via REST.
2. Publishes it to the `event-outcomes` Kafka topic.
3. The Kafka consumer picks up the message and queries the H2 database for unsettled bets matching the event ID.
4. For each matched bet, compares the user's pick (`selectedWinnerId`) against the actual outcome (`eventWinnerId`) to determine `WON` or `LOST`.
5. Marks the bets as settled (idempotency — duplicate events will find no unsettled bets).
6. After the DB transaction commits, emits settlement messages through a mocked RocketMQ producer that logs them.

Schema is managed through Flyway migrations. Demo seed data is loaded only in the `dev` profile.

## Tech Stack

| Category | Dependency | Purpose |
|----------|-----------|---------|
| Framework | Spring Boot 4.0.0 | Web, JPA, Kafka, Actuator, Validation |
| Database | H2 | In-memory database |
| Migrations | Flyway | Schema versioning and seed data |
| Messaging | Spring Kafka | Kafka producer/consumer |
| Observability | Micrometer + Prometheus | Custom counters, metrics scraping |
| Mapping | MapStruct 1.6.3 | Compile-time DTO/entity mapping |
| Boilerplate | Lombok 1.18.36 | `@Slf4j`, `@Getter`, `@RequiredArgsConstructor` |
| Testing | JUnit 5, Mockito, Awaitility, Testcontainers | Unit + integration tests with real Kafka |

## Project Structure

```
src/main/java/com/sporty/bettask/
├── controller/          REST endpoint + global exception handler
├── service/             Kafka publisher + bet settlement logic
├── messaging/           Kafka consumer, consumer config, RocketMQ producer
├── domain/              Immutable records (EventOutcomeMessage, BetSettlementMessage, SettlementResult)
├── dto/                 Request/response objects with validation
├── entity/              JPA entity (Bet)
├── repository/          Spring Data JPA repository
├── mapper/              MapStruct mappers (DTO <-> domain <-> entity)
└── exception/           Custom exceptions

src/main/resources/
├── db/migration/        Flyway versioned migrations (V1, V3, V4)
├── db/dev/              Repeatable seed data for dev profile
├── application.yml      Base config (Kafka, Actuator, metrics)
├── application-dev.yml  H2 in-memory + seed data
└── logback-spring.xml   JSON logging for stg/prod

src/test/
├── java/.../integration/   Integration tests (Testcontainers + Kafka)
├── java/.../service/       Unit tests for services
├── java/.../messaging/     Unit tests for consumer
├── java/.../controller/    Unit tests for controller
└── resources/              Test profile config + test seed data
```

## Prerequisites

- Docker and Docker Compose (required)
- SDKMAN (optional, for local development)

## Run With Docker Compose

From `bet-task/`:

```bash
docker compose up --build -d
```

This starts:

- Kafka on `localhost:9092`
- App on `localhost:8080`
- Prometheus on `localhost:9090`

To stop everything:

```bash
docker compose down
```

## Start the App Locally

Requires Kafka running on `localhost:9092` (e.g. via `docker compose up kafka -d`).

Set up Java 21 and Maven via SDKMAN:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

The `.sdkmanrc` file pins Java `21.0.9-tem` and Maven `3.9.12`.

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

By default, the app runs with the `dev` profile.

On startup:

- `db/migration` creates the shared schema
- `db/dev` seeds demo bets only for `dev`

Use a different profile with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=stg
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

For `stg` and `prod`, these environment variables are required:

```bash
export DB_URL=jdbc:postgresql://host:5432/bets
export DB_DRIVER_CLASS_NAME=org.postgresql.Driver
export DB_USERNAME=app_user
export DB_PASSWORD=secret
```

## Observability

- Health: `http://localhost:8080/actuator/health`
- Metrics list: `http://localhost:8080/actuator/metrics`
- Prometheus scrape: `http://localhost:8080/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- H2 Console (dev profile only): `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:bets`
  - Username: `sa`, no password

## Example Request

```bash
curl -i \
  -X POST http://localhost:8080/api/event-outcomes \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"EVT-1001","eventName":"Team A vs Team B","eventWinnerId":"TEAM-A"}'
```

Expected behavior:

- HTTP returns `202 Accepted`
- Kafka receives the event
- The consumer processes the message
- Matching bets are evaluated
- Mock RocketMQ payloads are logged
- Micrometer counters are updated and exposed on Prometheus endpoint

## Tests

Run all tests (requires Docker for integration tests via Testcontainers):

```bash
mvn test
```

Run unit tests only (no Docker required):

```bash
mvn test -Dtest='!*IntegrationTest,!*Integration*'
```

Run a single test class:

```bash
mvn test -Dtest=BetSettlementServiceTest
```

## IDE Setup

The project uses Lombok and MapStruct (compile-time annotation processors). Enable annotation processing in your IDE:

- **IntelliJ**: Settings > Build > Compiler > Annotation Processors > Enable

## Container Files

- App image: `Dockerfile`
- Compose stack: `docker-compose.yml`
- Prometheus config: `prometheus/prometheus.yml`
