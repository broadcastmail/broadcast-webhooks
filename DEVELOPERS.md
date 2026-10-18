# Developing broadcastmail-webhooks

## Prerequisites

- Java 25
- Maven 3.9+
- Docker (for Testcontainers)
- broadcastmail-api running and DB migrated
- Stripe CLI (for local webhook forwarding)

## Environment variables

```bash
# PostgreSQL
PGHOST=
PGPORT=5432
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=

# Stripe
STRIPE_WEBHOOK_SECRET=

# Resend
RESEND_WEBHOOK_SECRET=
```

## Running locally

```bash
./mvnw spring-boot:run
```

Webhooks service starts on `http://localhost:8082`.

To forward Stripe events locally:

```bash
stripe listen --forward-to localhost:8082/api/v1/webhooks/stripe
```

Note: Run broadcastmail-api first so Flyway migrations have run. Webhooks service has Flyway disabled.

## Running tests

```bash
./mvnw verify -Dgroups='!local'
```

## Architecture

See [ARCHITECTURE.md](https://github.com/broadcastmail/.github/blob/main/ARCHITECTURE.md).

## Database schema

See [DATABASE.md](https://github.com/broadcastmail/.github/blob/main/DATABASE.md).
