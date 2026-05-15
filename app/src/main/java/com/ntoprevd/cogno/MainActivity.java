package com.ntoprevd.cogno;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();

        // ---------- 透明状态栏 + 内容延伸到系统栏（与 Web 端 safe-area / 透明导航栏配合）----------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);

        webView = findViewById(R.id.main_webview);
        webView.setFitsSystemWindows(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
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

    private static boolean parseDarkFlagFromJsResult(String value) {
        if (value == null) return false;
        String s = value.trim();
        return "1".equals(s) || "\"1\"".equals(s);
    }

    /**
     * 同步状态栏 + 底部导航栏：导航栏背景全透明；图标/按钮颜色由 WindowInsetsController 控制。
     * 浅色：setAppearanceLightNavigationBars(true) → 深色图标；深色：false → 浅色图标。
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
     * JSBridge：网页可调用 Android.setNavigationBarColor(0) 将导航栏设为完全透明（ARGB，0 = 全透明）。
     */
    private class WebAppInterface {
        @JavascriptInterface
        public void setNavigationBarColor(int argb) {
            runOnUiThread(() -> getWindow().setNavigationBarColor(argb));
        }

        /**
         * 切换深浅主题时由网页调用：更新状态栏/导航栏前景对比度（导航栏底色仍保持透明）。
         */
        @JavascriptInterface
        public void setStatusBarDarkMode(boolean isDarkMode) {
            runOnUiThread(() -> applyThemeToSystemBars(isDarkMode));
        }
    }
}
