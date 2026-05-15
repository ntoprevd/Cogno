package com.ntoprevd.cogno.bridge;

import android.app.Activity;
import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.ntoprevd.cogno.BuildConfig;
import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import com.ntoprevd.cogno.data.repository.ChatRepository;
import com.ntoprevd.cogno.data.repository.ChatRepositoryImpl;
import com.ntoprevd.cogno.data.repository.OnResultCallback;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CognoJSBridge {

    public interface SystemBarController {
        void applyThemeToSystemBars(boolean isDarkMode);
    }

    private final Activity activity;
    private final WebView webView;
    private final ChatRepository chatRepository;
    private final SystemBarController systemBarController;

    public CognoJSBridge(Activity activity, WebView webView, SystemBarController systemBarController) {
        this.activity = activity;
        this.webView = webView;
        this.systemBarController = systemBarController;
        this.chatRepository = new ChatRepositoryImpl(activity.getApplicationContext());
    }

    @JavascriptInterface
    public void createSession(String title, String modelId, String callbackId) {
        long now = System.currentTimeMillis();
        SessionEntity session = new SessionEntity(
                UUID.randomUUID().toString(),
                normalizeTitle(title),
                emptyToNull(modelId),
                false,
                false,
                now,
                now,
                null
        );

        chatRepository.createSession(session, new OnResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                try {
                    sendSuccess(callbackId, toJson(session));
                } catch (JSONException e) {
                    sendError(callbackId, "SERIALIZE_ERROR", e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void sendMessage(String sessionId, String userContent, String callbackId) {
        if (isBlank(sessionId)) {
            sendError(callbackId, "INVALID_REQUEST", "sessionId is required.");
            return;
        }
        if (isBlank(userContent)) {
            sendError(callbackId, "INVALID_REQUEST", "userContent is required.");
            return;
        }

        chatRepository.getSessionById(sessionId, new OnResultCallback<SessionEntity>() {
            @Override
            public void onSuccess(SessionEntity session) {
                if (session == null) {
                    sendError(callbackId, "SESSION_NOT_FOUND", "Session does not exist.");
                    return;
                }
                insertUserMessage(session, userContent, callbackId);
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void getMessages(String sessionId, int page, int limit, String callbackId) {
        if (isBlank(sessionId)) {
            sendError(callbackId, "INVALID_REQUEST", "sessionId is required.");
            return;
        }

        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safePage = Math.max(0, page);
        int offset = safePage * safeLimit;
        chatRepository.getMessagesBySessionId(sessionId, safeLimit, offset, new OnResultCallback<List<MessageEntity>>() {
            @Override
            public void onSuccess(List<MessageEntity> messages) {
                try {
                    JSONObject data = new JSONObject()
                            .put("sessionId", sessionId)
                            .put("page", safePage)
                            .put("limit", safeLimit)
                            .put("messages", messagesToJson(messages));
                    sendSuccess(callbackId, data);
                } catch (JSONException e) {
                    sendError(callbackId, "SERIALIZE_ERROR", e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void getAllSessions(String callbackId) {
        chatRepository.getAllSessions(new OnResultCallback<List<SessionEntity>>() {
            @Override
            public void onSuccess(List<SessionEntity> sessions) {
                try {
                    sendSuccess(callbackId, new JSONObject().put("sessions", sessionsToJson(sessions)));
                } catch (JSONException e) {
                    sendError(callbackId, "SERIALIZE_ERROR", e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public String postMessage(String requestJson) {
        String requestId = null;
        try {
            JSONObject request = new JSONObject(requestJson == null ? "" : requestJson);
            int version = request.optInt("version", 1);
            requestId = request.optString("requestId", null);
            String command = request.optString("command", "");
            JSONObject payload = request.optJSONObject("payload");
            String callbackId = request.optString("callbackId", null);

            if (version != 1) {
                return error(requestId, "UNSUPPORTED_VERSION", "Only JSBridge protocol version 1 is supported.").toString();
            }
            if (command.isEmpty()) {
                return error(requestId, "INVALID_REQUEST", "Missing command.").toString();
            }

            switch (command) {
                case "system.getCapabilities":
                    return success(requestId, getCapabilities()).toString();
                case "ui.setSystemBars":
                    return handleSetSystemBars(requestId, payload).toString();
                case "chat.createSession":
                    createSession(payloadString(payload, "title"), payloadString(payload, "modelId"), callbackId);
                    return accepted(requestId).toString();
                case "chat.sendMessage":
                    sendMessage(payloadString(payload, "sessionId"), payloadString(payload, "userContent"), callbackId);
                    return accepted(requestId).toString();
                case "chat.getMessages":
                    getMessages(
                            payloadString(payload, "sessionId"),
                            payload == null ? 0 : payload.optInt("page", 0),
                            payload == null ? 20 : payload.optInt("limit", 20),
                            callbackId
                    );
                    return accepted(requestId).toString();
                case "chat.listSessions":
                case "session.getAllSessions":
                    getAllSessions(callbackId);
                    return accepted(requestId).toString();
                default:
                    return error(requestId, "UNKNOWN_COMMAND", "Unknown command: " + command).toString();
            }
        } catch (JSONException e) {
            return error(requestId, "INVALID_REQUEST", "Request must be valid JSON.").toString();
        } catch (Exception e) {
            return error(requestId, "INTERNAL_ERROR", "Native bridge failed.").toString();
        }
    }

    @JavascriptInterface
    public void setNavigationBarColor(int argb) {
        activity.runOnUiThread(() -> activity.getWindow().setNavigationBarColor(argb));
    }

    @JavascriptInterface
    public void setStatusBarDarkMode(boolean isDarkMode) {
        activity.runOnUiThread(() -> systemBarController.applyThemeToSystemBars(isDarkMode));
    }

    private void insertUserMessage(SessionEntity session, String userContent, String callbackId) {
        long now = System.currentTimeMillis();
        MessageEntity message = new MessageEntity(
                UUID.randomUUID().toString(),
                session.id,
                "user",
                userContent,
                "completed",
                null,
                null,
                now,
                now
        );

        chatRepository.insertMessage(message, new OnResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                SessionEntity updatedSession = new SessionEntity(
                        session.id,
                        session.title,
                        session.modelId,
                        session.pinned,
                        session.archived,
                        session.createdAt,
                        now,
                        preview(userContent)
                );
                updateSessionAfterMessage(updatedSession, message, callbackId);
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    private void updateSessionAfterMessage(SessionEntity session, MessageEntity message, String callbackId) {
        chatRepository.updateSession(session, new OnResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                try {
                    JSONObject data = new JSONObject()
                            .put("session", toJson(session))
                            .put("message", toJson(message));
                    sendSuccess(callbackId, data);
                } catch (JSONException e) {
                    sendError(callbackId, "SERIALIZE_ERROR", e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                sendError(callbackId, "DB_ERROR", e.getMessage());
            }
        });
    }

    private JSONObject handleSetSystemBars(String requestId, JSONObject payload) throws JSONException {
        if (payload == null) {
            return error(requestId, "INVALID_REQUEST", "Missing payload.");
        }

        boolean darkMode = payload.optBoolean("darkMode", false);
        boolean hasNavigationBarColor = payload.has("navigationBarColor");
        int navigationBarColor = payload.optInt("navigationBarColor", Color.TRANSPARENT);
        activity.runOnUiThread(() -> {
            systemBarController.applyThemeToSystemBars(darkMode);
            if (hasNavigationBarColor) {
                activity.getWindow().setNavigationBarColor(navigationBarColor);
            }
        });
        return success(requestId, new JSONObject().put("applied", true));
    }

    private JSONObject getCapabilities() throws JSONException {
        JSONArray commands = new JSONArray()
                .put("system.getCapabilities")
                .put("ui.setSystemBars")
                .put("chat.createSession")
                .put("chat.listSessions")
                .put("chat.getMessages")
                .put("chat.sendMessage")
                .put("session.getAllSessions");

        return new JSONObject()
                .put("protocolVersion", 1)
                .put("devMode", BuildConfig.WEBVIEW_DEV_MODE)
                .put("commands", commands);
    }

    private void sendSuccess(String callbackId, JSONObject data) {
        try {
            sendCallback(callbackId, new JSONObject()
                    .put("ok", true)
                    .put("data", data == null ? new JSONObject() : data));
        } catch (JSONException e) {
            sendError(callbackId, "SERIALIZE_ERROR", e.getMessage());
        }
    }

    private void sendError(String callbackId, String code, String message) {
        try {
            sendCallback(callbackId, new JSONObject()
                    .put("ok", false)
                    .put("error", new JSONObject()
                            .put("code", code)
                            .put("message", message == null ? "" : message)));
        } catch (JSONException ignored) {
            // Last-resort callback failure is intentionally swallowed; callers still see console logs.
        }
    }

    private void sendCallback(String callbackId, JSONObject response) {
        String safeCallbackId = callbackId == null ? "" : callbackId;
        String responseJson = response.toString();
        String script = "(function(){"
                + "var id=" + JSONObject.quote(safeCallbackId) + ";"
                + "var payload=" + responseJson + ";"
                + "try{"
                + "if(id&&window.CognoBridgeCallbacks&&typeof window.CognoBridgeCallbacks[id]==='function'){window.CognoBridgeCallbacks[id](payload);return;}"
                + "if(id&&typeof window[id]==='function'){window[id](payload);return;}"
                + "console.log('[CognoJSBridge]', payload);"
                + "}catch(e){console.error('[CognoJSBridge callback error]',e,payload);}"
                + "})();";
        activity.runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private static JSONObject accepted(String requestId) throws JSONException {
        return success(requestId, new JSONObject().put("accepted", true));
    }

    private static JSONObject success(String requestId, JSONObject data) throws JSONException {
        return new JSONObject()
                .put("version", 1)
                .put("requestId", requestId == null ? JSONObject.NULL : requestId)
                .put("ok", true)
                .put("data", data == null ? new JSONObject() : data);
    }

    private static JSONObject error(String requestId, String code, String message) {
        try {
            return new JSONObject()
                    .put("version", 1)
                    .put("requestId", requestId == null ? JSONObject.NULL : requestId)
                    .put("ok", false)
                    .put("error", new JSONObject()
                            .put("code", code)
                            .put("message", message == null ? "" : message));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static JSONObject toJson(SessionEntity session) throws JSONException {
        return new JSONObject()
                .put("id", session.id)
                .put("title", session.title)
                .put("modelId", session.modelId == null ? JSONObject.NULL : session.modelId)
                .put("pinned", session.pinned)
                .put("archived", session.archived)
                .put("createdAt", session.createdAt)
                .put("updatedAt", session.updatedAt)
                .put("lastMessagePreview", session.lastMessagePreview == null ? JSONObject.NULL : session.lastMessagePreview);
    }

    private static JSONObject toJson(MessageEntity message) throws JSONException {
        return new JSONObject()
                .put("id", message.id)
                .put("sessionId", message.sessionId)
                .put("role", message.role)
                .put("content", message.content)
                .put("status", message.status)
                .put("errorCode", message.errorCode == null ? JSONObject.NULL : message.errorCode)
                .put("tokenCount", message.tokenCount == null ? JSONObject.NULL : message.tokenCount)
                .put("createdAt", message.createdAt)
                .put("updatedAt", message.updatedAt);
    }

    private static JSONArray sessionsToJson(List<SessionEntity> sessions) throws JSONException {
        JSONArray array = new JSONArray();
        for (SessionEntity session : sessions) {
            array.put(toJson(session));
        }
        return array;
    }

    private static JSONArray messagesToJson(List<MessageEntity> messages) throws JSONException {
        JSONArray array = new JSONArray();
        for (MessageEntity message : messages) {
            array.put(toJson(message));
        }
        return array;
    }

    private static String payloadString(JSONObject payload, String key) {
        if (payload == null || payload.isNull(key)) {
            return null;
        }
        return payload.optString(key, null);
    }

    private static String normalizeTitle(String title) {
        if (isBlank(title)) {
            return "新会话";
        }
        return title.trim();
    }

    private static String emptyToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private static String preview(String content) {
        String trimmed = content.trim();
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(0, 80);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
