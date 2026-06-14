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
