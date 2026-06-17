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

## Recommended: Alibaba Cloud Function Compute

Alibaba Cloud Function Compute is the recommended gateway host for users in mainland China. Deploy
the Node.js gateway in a mainland region such as Hangzhou or Shanghai, and keep the temporary image
bucket in Alibaba Cloud OSS Hong Kong.

Dashboard deployment:

1. Open Alibaba Cloud Function Compute and create a Web Function.
2. Select `Node.js 20 / Custom Runtime / Debian 11`.
3. Use command mode with `npm start`.
4. Set the listening port to `8787`.
5. Set the timeout to at least `120` seconds.
6. Upload a ZIP whose top level contains `package.json`, `src/`, and `config/`.
7. Configure these environment variables:
   - `GLM_API_KEY`
   - `COGNO_APP_TOKEN`
   - `OSS_ACCESS_KEY_ID` (RAM user AccessKey ID)
   - `OSS_ACCESS_KEY_SECRET` (RAM user AccessKey secret)
   - `COGNO_GATEWAY_ENABLED=true`
   - `MAX_INPUT_CHARS=60000`
   - `MAX_OUTPUT_TOKENS=8192`
   - `MAX_IMAGE_BYTES=5242880`
   - `IMAGE_URL_TTL_SECONDS=900`
   - `OSS_BUCKET=cogno-temporary-images`
   - `OSS_ENDPOINT=https://oss-cn-hongkong.aliyuncs.com`
8. Create a private Alibaba Cloud OSS bucket named `cogno-temporary-images` in Hong Kong.
9. Add an OSS lifecycle rule that deletes objects with prefix `temporary/` after 1 day.
10. Use the Function Compute domain plus `/v1` as Android's `COGNO_EXPERIENCE_BASE_URL`.

The Android app token must match `COGNO_APP_TOKEN`. Provider and OSS keys remain only in the
Function Compute environment variables.

`COGNO_APP_TOKEN` is embedded in the demo APK and must not be treated as a secret or user
authentication mechanism. For a public deployment, enable platform-side rate limiting (preferably by
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

The gateway uploads temporary images to the private OSS bucket by using a restricted RAM user.
It returns a 15-minute OSS signed GET URL to the Android app, which is then passed to the vision
model. Never place the RAM AccessKey pair in the Android app or commit it to this repository.

Limits are intentionally loose for classroom testing and can be changed through environment variables.
