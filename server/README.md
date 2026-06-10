# Cogno Model Gateway

Node.js 20+ model proxy for Cogno's developer-sponsored experience models.

## Local setup

1. Copy `.env.example` values into your shell or deployment platform.
2. Set `GLM_API_KEY` to the provider key.
3. Set `COGNO_APP_TOKEN` to a random value.
4. Run `npm start`.

The server intentionally has no npm runtime dependencies.

## Railway deployment

1. Push this repository to GitHub.
2. Create a Railway project from the GitHub repository.
3. Set the service root directory to `/server`.
4. Add the environment variables from `.env.example`.
5. Generate a Railway public domain.
6. Use `https://<railway-domain>/v1` as `COGNO_EXPERIENCE_BASE_URL`.

## Recommended: Cloudflare Workers

Cloudflare Workers is the simplest long-lived option for this project. The free plan does not sleep and currently includes 100,000 requests per day.

Dashboard deployment:

1. Open Cloudflare Dashboard > Workers & Pages > Create Worker.
2. Replace the generated code with `cloudflare-worker.js`.
3. In Settings > Variables and Secrets, add encrypted secrets:
   - `GLM_API_KEY`
   - `COGNO_APP_TOKEN`
4. Add normal variables:
   - `COGNO_GATEWAY_ENABLED=true`
   - `MAX_INPUT_CHARS=60000`
   - `MAX_OUTPUT_TOKENS=8192`
5. Deploy and copy the generated `workers.dev` URL.
6. Append `/v1` to that URL for Android's `COGNO_EXPERIENCE_BASE_URL`.

The Android app token must match the Worker secret. The GLM key remains only inside Cloudflare.

## Android configuration

Add these entries to the project-level `local.properties`:

```properties
COGNO_EXPERIENCE_BASE_URL=https://your-domain.example/v1
COGNO_EXPERIENCE_APP_TOKEN=the-same-app-token
```

`local.properties` must remain local. Provider keys must never be placed in the Android project or APK.

## Endpoints

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`

Limits are intentionally loose for classroom testing and can be changed through environment variables.
