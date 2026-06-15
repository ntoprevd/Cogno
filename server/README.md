# Cogno Model Gateway

Node.js 20+ model proxy for Cogno's developer-sponsored experience models.

## Local setup

1. Copy `.env.example` values into your shell or deployment platform.
2. Set `GLM_API_KEY` to the provider key.
3. Set `COGNO_APP_TOKEN` to a random value used as a coarse demo-environment gate.
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
   - `OSS_ACCESS_KEY_ID` (RAM user AccessKey ID)
   - `OSS_ACCESS_KEY_SECRET` (RAM user AccessKey secret)
4. Create a private Alibaba Cloud OSS bucket named `cogno-temporary-images` in Hong Kong.
5. Add an OSS lifecycle rule that deletes objects with prefix `temporary/` after 1 day.
6. Add normal variables:
   - `COGNO_GATEWAY_ENABLED=true`
   - `MAX_INPUT_CHARS=60000`
   - `MAX_OUTPUT_TOKENS=8192`
   - `MAX_IMAGE_BYTES=5242880`
   - `IMAGE_URL_TTL_SECONDS=900`
   - `OSS_BUCKET=cogno-temporary-images`
   - `OSS_ENDPOINT=https://oss-cn-hongkong.aliyuncs.com`
7. Deploy and copy the generated `workers.dev` URL.
8. Append `/v1` to that URL for Android's `COGNO_EXPERIENCE_BASE_URL`.

The Android app token must match the Worker secret. The GLM key remains only inside Cloudflare.

`COGNO_APP_TOKEN` is embedded in the demo APK and must not be treated as a secret or user
authentication mechanism. For a public deployment, enable Cloudflare Rate Limiting (preferably by
IP and route) or replace the demo gate with account-backed short-lived tokens. The Node gateway
uses the request source IP for its in-memory daily demo quota and does not require a device ID.

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
- `POST /v1/images`
- `POST /v1/chat/completions`

The Worker uploads temporary images to the private OSS bucket by using a restricted RAM user.
It returns a 15-minute OSS signed GET URL to the Android app, which is then passed to the vision
model. Never place the RAM AccessKey pair in the Android app or commit it to this repository.

Limits are intentionally loose for classroom testing and can be changed through environment variables.
