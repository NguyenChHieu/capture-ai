# Infrastructure (local vs production)

| Component | Local (`docker compose`) | Production |
|-----------|--------------------------|------------|
| Postgres + pgvector | `pgvector/pgvector:pg16` on `:5432` | Neon with pooler |
| Kafka | Redpanda `:9092` | Confluent Cloud or Redpanda Cloud |
| Temporal | `temporalio/auto-setup` `:7233` | Temporal Cloud namespace |
| API + worker | `./gradlew :api:bootRun` / `:worker:bootRun` | Railway/Fly.io Docker |
| Web | `npm run dev` in `frontend/` | Vercel or Netlify |
| Object storage | optional LocalStack | Cloudflare R2 |

After `docker compose up -d`, create topics:

```powershell
.\scripts\init-kafka-topics.ps1
```

Environment variables for API/worker: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP`, `TEMPORAL_TARGET`, `JWT_SECRET`, `OPENAI_API_KEY` (optional).
