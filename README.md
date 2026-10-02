# Capture Organizer

Personal scroll-capture organizer: Spring Boot + Kafka + Temporal + React + browser extension.

See [Infrastructure](docs/INFRA.md) for production mapping.

## Local infrastructure

```bash
docker compose up -d
```

| Service | Local | Production equivalent |
|---------|-------|------------------------|
| Postgres + pgvector | `localhost:5432` | Neon |
| Kafka | Redpanda `localhost:9092` | Confluent / Redpanda Cloud |
| Temporal | `localhost:7233` | Temporal Cloud |

Create Kafka topics (first run):

```bash
docker compose exec redpanda rpk topic create capture.ingested capture.processed capture.ingested.dlq
```

## Backend

```bash
cd backend
set DB_HOST=localhost
set KAFKA_BOOTSTRAP=localhost:9092
set TEMPORAL_TARGET=127.0.0.1:7233
.\gradlew.bat :api:bootRun
# separate terminal
.\gradlew.bat :worker:bootRun
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_URL=http://localhost:8080`

Deploy to Vercel or Netlify (static build).

## Extension

```bash
cd extension
npm install
npm run dev
```

Load unpacked extension from `extension/.output/chrome-mv3` (WXT build output). Set API URL and PAT in popup.

## Docs

- [Taxonomy samples](docs/taxonomy-and-samples.md)
- [Privacy](docs/PRIVACY.md)
- [Mobile / iOS Shortcut](docs/MOBILE-IOS.md)
- [Extension security](docs/EXTENSION.md)
