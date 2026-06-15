const MODELS = [
  {
    id: "glm-4.5-air",
    label: "GLM-4.5-Air",
    description: "深度思考",
    upstreamModel: "glm-4.5-air"
  },
  {
    id: "glm-4.6v",
    label: "GLM-4.6V",
    description: "视觉理解",
    upstreamModel: "glm-4.6v"
  },
  {
    id: "glm-4-flash",
    label: "GLM-4-Flash",
    description: "文本生成",
    upstreamModel: "glm-4-flash"
  }
];

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, enabled: env.COGNO_GATEWAY_ENABLED !== "false", models: MODELS.length });
    }

    if (request.method === "GET" && url.pathname.startsWith("/v1/images/")) {
      return serveSignedImage(url, env);
    }

    if (env.COGNO_GATEWAY_ENABLED === "false") {
      return errorResponse(503, "体验模型当前已停用");
    }

    if (env.COGNO_APP_TOKEN && request.headers.get("Authorization") !== `Bearer ${env.COGNO_APP_TOKEN}`) {
      return errorResponse(401, "无效的 App 访问令牌");
    }

    if (request.method === "GET" && url.pathname === "/v1/models") {
      return json({
        data: MODELS.map(({ upstreamModel, ...model }) => model)
      });
    }

    if (request.method === "POST" && url.pathname === "/v1/images") {
      return uploadTemporaryImage(request, url, env);
    }

    if (request.method !== "POST" || url.pathname !== "/v1/chat/completions") {
      return errorResponse(404, "接口不存在");
    }

    if (!env.GLM_API_KEY) {
      return errorResponse(503, "GLM API Key 未配置");
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return errorResponse(400, "请求体不是有效 JSON");
    }

    const model = MODELS.find((item) => item.id === body.model);
    if (!model) return errorResponse(400, "该体验模型不存在");

    const maxInputChars = Number(env.MAX_INPUT_CHARS || 60000);
    if (JSON.stringify(body.messages || []).length > maxInputChars) {
      return errorResponse(413, "输入内容过长");
    }

    const upstreamResponse = await fetch(
      "https://open.bigmodel.cn/api/paas/v4/chat/completions",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${env.GLM_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          ...body,
          model: model.upstreamModel,
          max_tokens: Math.min(
            Number(body.max_tokens || env.MAX_OUTPUT_TOKENS || 8192),
            Number(env.MAX_OUTPUT_TOKENS || 8192)
          )
        })
      }
    );

    const headers = new Headers(upstreamResponse.headers);
    for (const [name, value] of Object.entries(corsHeaders())) {
      headers.set(name, value);
    }
    headers.set("Cache-Control", "no-cache");

    // Response body is passed through directly, so SSE streaming remains streaming.
    return new Response(upstreamResponse.body, {
      status: upstreamResponse.status,
      headers
    });
  }
};

async function uploadTemporaryImage(request, url, env) {
  if (!hasOssConfig(env)) {
    return errorResponse(503, "图片对象存储尚未配置");
  }
  const contentType = request.headers.get("Content-Type") || "";
  if (!["image/jpeg", "image/png", "image/webp"].includes(contentType)) {
    return errorResponse(415, "仅支持 JPEG、PNG 或 WebP 图片");
  }
  const bytes = await request.arrayBuffer();
  const maxBytes = Number(env.MAX_IMAGE_BYTES || 5 * 1024 * 1024);
  if (bytes.byteLength === 0 || bytes.byteLength > maxBytes) {
    return errorResponse(413, "图片大小超出限制");
  }

  const extension = contentType === "image/png" ? "png" : contentType === "image/webp" ? "webp" : "jpg";
  const key = `temporary/${crypto.randomUUID()}.${extension}`;
  const uploadResponse = await putOssObject(env, key, bytes, contentType);
  if (!uploadResponse.ok) {
    const ossError = await uploadResponse.text();
    const ossCode = ossError.match(/<Code>([^<]+)<\/Code>/)?.[1] || "UnknownError";
    // 只返回 OSS 错误码，不暴露请求签名、AccessKey 或完整上游响应。
    return errorResponse(
      502,
      `图片上传至 OSS 失败：${ossCode}（HTTP ${uploadResponse.status}）`
    );
  }
  const expires = Math.floor(Date.now() / 1000) + Number(env.IMAGE_URL_TTL_SECONDS || 900);
  return json({
    url: await createOssSignedGetUrl(env, key, expires),
    expires_at: expires
  });
}

async function serveSignedImage(url, env) {
  return errorResponse(410, "图片访问已迁移至阿里云 OSS 临时签名地址");
}

function hasOssConfig(env) {
  return Boolean(
    env.OSS_ACCESS_KEY_ID &&
    env.OSS_ACCESS_KEY_SECRET &&
    env.OSS_BUCKET &&
    env.OSS_ENDPOINT
  );
}

async function putOssObject(env, key, bytes, contentType) {
  const date = new Date().toUTCString();
  const canonicalResource = `/${env.OSS_BUCKET}/${key}`;
  const stringToSign = `PUT\n\n${contentType}\n${date}\n${canonicalResource}`;
  const signature = await hmacSha1Base64(env.OSS_ACCESS_KEY_SECRET, stringToSign);
  return fetch(ossObjectUrl(env, key), {
    method: "PUT",
    headers: {
      Authorization: `OSS ${env.OSS_ACCESS_KEY_ID}:${signature}`,
      "Content-Type": contentType,
      Date: date
    },
    body: bytes
  });
}

async function createOssSignedGetUrl(env, key, expires) {
  const canonicalResource = `/${env.OSS_BUCKET}/${key}`;
  const stringToSign = `GET\n\n\n${expires}\n${canonicalResource}`;
  const signature = await hmacSha1Base64(env.OSS_ACCESS_KEY_SECRET, stringToSign);
  const params = new URLSearchParams({
    OSSAccessKeyId: env.OSS_ACCESS_KEY_ID,
    Expires: String(expires),
    Signature: signature
  });
  return `${ossObjectUrl(env, key)}?${params}`;
}

function ossObjectUrl(env, key) {
  const endpointHost = env.OSS_ENDPOINT
    .replace(/^https?:\/\//, "")
    .replace(/\/+$/, "");
  const encodedKey = key.split("/").map(encodeURIComponent).join("/");
  return `https://${env.OSS_BUCKET}.${endpointHost}/${encodedKey}`;
}

async function hmacSha1Base64(secret, value) {
  const encoder = new TextEncoder();
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    cryptoKey,
    encoder.encode(value)
  );
  return arrayBufferToBase64(signature);
}

function arrayBufferToBase64(value) {
  const bytes = new Uint8Array(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
  };
}

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      ...corsHeaders(),
      "Content-Type": "application/json; charset=utf-8"
    }
  });
}

function errorResponse(status, message) {
  return json({ error: { message } }, status);
}
