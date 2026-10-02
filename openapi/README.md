# OpenAPI

Run the API and open:

- JSON: http://localhost:8080/v3/api-docs
- UI: http://localhost:8080/swagger-ui.html

Generate TypeScript types (optional):

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o frontend/src/schema.d.ts
```
