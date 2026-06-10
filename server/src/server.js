import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const models = JSON.parse(
  await readFile(join(here, "..", "config", "models.json"), "utf8")
);
const enabledModels = models.filter((model) => model.enabled);
const usageByDevice = new Map();

const config = {
  host: env("HOST", "0.0.0.0"),
  port: integerEnv("PORT", 8787),
  enabled: env("COGNO_GATEWAY_ENABLED", "true") === "true",
  appToken: env("COGNO_APP_TOKEN", ""),
  dailyRequestLimit: integerEnv("DAILY_REQUEST_LIMIT", 200),
  dailyTokenLimit: integerEnv("DAILY_TOKEN_LIMIT", 1_000_000),
  maxInputChars: integerEnv("MAX_INPUT_CHARS", 60_000),
  maxOutputTokens: integerEnv("MAX_OUTPUT_TOKENS", 8192)
};

const providers = {
  glm: {
    baseUrl: "https://open.bigmodel.cn/api/paas/v4",
    apiKey: env("GLM_API_KEY", "")
  }
};

const server = createServer(async (request, response) => {
  setCorsHeaders(response);
  if (request.method === "OPTIONS") {
    response.writeHead(204).end();
    return;
  }

  try {
    const url = new URL(request.url ?? "/", "http://localhost");
    if (request.method === "GET" && url.pathname === "/health") {
      sendJson(response, 200, {
        ok: true,
        enabled: config.enabled,
        models: enabledModels.length
      });
      return;
    }

    authorize(request);
    if (!config.enabled) throw httpError(503, "体验模型当前已停用");

    if (request.method === "GET" && url.pathname === "/v1/models") {
      sendJson(response, 200, {
        data: enabledModels.map(({ provider, upstreamModel, enabled, ...model }) => model)
      });
      return;
    }

    if (request.method === "POST" && url.pathname === "/v1/chat/completions") {
      await proxyChatCompletion(request, response);
      return;
    }

    throw httpError(404, "接口不存在");
  } catch (error) {
    if (!response.headersSent) {
      sendJson(response, error.statusCode ?? 500, {
        error: { message: error.message ?? "服务器内部错误" }
      });
    } else {
      response.end();
    }
  }
});

server.listen(config.port, config.host, () => {
  console.log(`Cogno gateway listening on http://${config.host}:${config.port}`);
});

async function proxyChatCompletion(request, response) {
  const body = await readJsonBody(request);
  const model = enabledModels.find((candidate) => candidate.id === body.model);
  if (!model) throw httpError(400, "该体验模型不存在或已停用");

  const provider = providers[model.provider];
  if (!provider?.apiKey) throw httpError(503, `${model.provider} API Key 未配置`);

  const inputChars = JSON.stringify(body.messages ?? []).length;
  if (inputChars > config.maxInputChars) throw httpError(413, "输入内容过长");

  const deviceId = String(request.headers["x-cogno-device-id"] ?? "unknown");
  const usage = getTodayUsage(deviceId);
  if (usage.requests >= config.dailyRequestLimit) throw httpError(429, "今日体验请求次数已用完");
  if (usage.tokens >= config.dailyTokenLimit) throw httpError(429, "今日体验 Token 已用完");

  usage.requests += 1;
  const upstreamBody = {
    ...body,
    model: model.upstreamModel,
    max_tokens: Math.min(
      Number(body.max_tokens ?? config.maxOutputTokens),
      config.maxOutputTokens
    )
  };

  const upstream = await fetch(`${provider.baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${provider.apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(upstreamBody)
  });

  response.statusCode = upstream.status;
  response.setHeader(
    "Content-Type",
    upstream.headers.get("content-type") ?? "application/json; charset=utf-8"
  );
  response.setHeader("Cache-Control", "no-cache");

  if (!upstream.body) {
    response.end();
    return;
  }

  if (body.stream) {
    const reader = upstream.body.getReader();
    const decoder = new TextDecoder();
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const text = decoder.decode(value, { stream: true });
      usage.tokens += extractSseTokens(text);
      response.write(value);
    }
    response.end();
    return;
  }

  const text = await upstream.text();
  usage.tokens += extractJsonTokens(text);
  response.end(text);
}

function authorize(request) {
  if (!config.appToken) return;
  const auth = String(request.headers.authorization ?? "");
  if (auth !== `Bearer ${config.appToken}`) throw httpError(401, "无效的 App 访问令牌");
}

async function readJsonBody(request) {
  let raw = "";
  for await (const chunk of request) {
    raw += chunk;
    if (raw.length > config.maxInputChars * 2) throw httpError(413, "请求体过大");
  }
  try {
    return JSON.parse(raw);
  } catch {
    throw httpError(400, "请求体不是有效 JSON");
  }
}

function getTodayUsage(deviceId) {
  const day = new Date().toISOString().slice(0, 10);
  const key = `${day}:${deviceId}`;
  if (!usageByDevice.has(key)) usageByDevice.set(key, { requests: 0, tokens: 0 });
  return usageByDevice.get(key);
}

function extractJsonTokens(text) {
  try {
    return Number(JSON.parse(text)?.usage?.total_tokens ?? 0);
  } catch {
    return 0;
  }
}

function extractSseTokens(text) {
  return text.split("\n").reduce((total, line) => {
    if (!line.startsWith("data:")) return total;
    const data = line.slice(5).trim();
    if (!data || data === "[DONE]") return total;
    return total + extractJsonTokens(data);
  }, 0);
}

function setCorsHeaders(response) {
  response.setHeader("Access-Control-Allow-Origin", "*");
  response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Cogno-Device-Id");
  response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
}

function sendJson(response, statusCode, value) {
  response.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(value));
}

function httpError(statusCode, message) {
  const error = new Error(message);
  error.statusCode = statusCode;
  return error;
}

function env(name, fallback) {
  return globalThis.process?.env?.[name] ?? fallback;
}

function integerEnv(name, fallback) {
  const value = Number.parseInt(env(name, ""), 10);
  return Number.isFinite(value) ? value : fallback;
}
