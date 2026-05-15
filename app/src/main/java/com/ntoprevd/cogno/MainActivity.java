package com.ntoprevd.cogno;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();

        // Transparent system bars; the Web layer handles safe-area spacing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);

        webView = findViewById(R.id.main_webview);
        webView.setFitsSystemWindows(false);
        configureWebView(webView);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("file:///android_asset/")) {
                    return false;
                }
                return !BuildConfig.WEBVIEW_DEV_MODE;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                        "(function(){try{return localStorage.getItem('cogno-dark-mode')==='true'?1:0}catch(e){return 0}})()",
                        value -> {
                            boolean dark = parseDarkFlagFromJsResult(value);
                            runOnUiThread(() -> applyThemeToSystemBars(dark));
                        });
            }
        });
        webView.loadUrl("file:///android_asset/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void configureWebView(WebView target) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.WEBVIEW_DEV_MODE);

        WebSettings settings = target.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);

        // Asset pages remain loadable; broader file/content access is only kept for debug inspection.
        settings.setAllowFileAccess(BuildConfig.WEBVIEW_DEV_MODE);
        settings.setAllowContentAccess(BuildConfig.WEBVIEW_DEV_MODE);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(BuildConfig.WEBVIEW_DEV_MODE
                ? WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    }

    private static boolean parseDarkFlagFromJsResult(String value) {
        if (value == null) return false;
        String s = value.trim();
        return "1".equals(s) || "\"1\"".equals(s);
    }

    /**
     * Sync status/navigation bar contrast with the Web theme.
     */
    private void applyThemeToSystemBars(boolean isDark) {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (isDark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isDark) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    /**
     * JSBridge entrypoint. New code should use postMessage(JSON); legacy theme methods stay
     * available until the front end is migrated.
     */
    private class WebAppInterface {
        @JavascriptInterface
        public String postMessage(String requestJson) {
            String requestId = null;
            try {
                JSONObject request = new JSONObject(requestJson == null ? "" : requestJson);
                int version = request.optInt("version", 1);
                requestId = request.optString("requestId", null);
                String command = request.optString("command", "");
                JSONObject payload = request.optJSONObject("payload");

                if (version != 1) {
                    return error(requestId, "UNSUPPORTED_VERSION", "Only JSBridge protocol version 1 is supported.");
                }
                if (command.isEmpty()) {
                    return error(requestId, "INVALID_REQUEST", "Missing command.");
                }

                switch (command) {
                    case "system.getCapabilities":
                        return success(requestId, getCapabilities());
                    case "ui.setSystemBars":
                        return handleSetSystemBars(requestId, payload);
                    case "chat.createSession":
                    case "chat.listSessions":
                    case "chat.getMessages":
                    case "chat.sendMessage":
                    case "chat.cancelStream":
                    case "note.list":
                    case "note.get":
                    case "note.save":
                    case "note.generateFromSession":
                    case "setting.get":
                    case "setting.set":
                    case "attachment.pickFile":
                    case "voice.startAsr":
                    case "voice.stopAsr":
                        return error(requestId, "NOT_IMPLEMENTED", "Command is defined but not implemented yet.");
                    default:
                        return error(requestId, "UNKNOWN_COMMAND", "Unknown command: " + command);
                }
            } catch (JSONException e) {
                return error(requestId, "INVALID_REQUEST", "Request must be valid JSON.");
            } catch (Exception e) {
                return error(requestId, "INTERNAL_ERROR", "Native bridge failed.");
            }
        }

        @JavascriptInterface
        public void setNavigationBarColor(int argb) {
            runOnUiThread(() -> getWindow().setNavigationBarColor(argb));
        }

        @JavascriptInterface
        public void setStatusBarDarkMode(boolean isDarkMode) {
            runOnUiThread(() -> applyThemeToSystemBars(isDarkMode));
        }

        private String handleSetSystemBars(String requestId, JSONObject payload) throws JSONException {
            if (payload == null) {
                return error(requestId, "INVALID_REQUEST", "Missing payload.");
            }

            boolean darkMode = payload.optBoolean("darkMode", false);
            boolean hasNavigationBarColor = payload.has("navigationBarColor");
            int navigationBarColor = payload.optInt("navigationBarColor", Color.TRANSPARENT);
            runOnUiThread(() -> {
                applyThemeToSystemBars(darkMode);
                if (hasNavigationBarColor) {
                    getWindow().setNavigationBarColor(navigationBarColor);
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
                    .put("chat.cancelStream")
                    .put("note.list")
                    .put("note.get")
                    .put("note.save")
                    .put("note.generateFromSession")
                    .put("setting.get")
                    .put("setting.set")
                    .put("attachment.pickFile")
                    .put("voice.startAsr")
                    .put("voice.stopAsr");

            return new JSONObject()
                    .put("protocolVersion", 1)
                    .put("devMode", BuildConfig.WEBVIEW_DEV_MODE)
                    .put("commands", commands);
        }

        private String success(String requestId, JSONObject data) {
            try {
                return new JSONObject()
                        .put("version", 1)
                        .put("requestId", requestId == null ? JSONObject.NULL : requestId)
                        .put("ok", true)
                        .put("data", data == null ? new JSONObject() : data)
                        .toString();
            } catch (JSONException e) {
                return "{\"version\":1,\"ok\":false,\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"Native bridge failed.\"}}";
            }
        }

        private String error(String requestId, String code, String message) {
            try {
                JSONObject error = new JSONObject()
                        .put("code", code)
                        .put("message", message);
                return new JSONObject()
                        .put("version", 1)
                        .put("requestId", requestId == null ? JSONObject.NULL : requestId)
                        .put("ok", false)
                        .put("error", error)
                        .toString();
            } catch (JSONException e) {
                return "{\"version\":1,\"ok\":false,\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"Native bridge failed.\"}}";
            }
        }
    }
}
