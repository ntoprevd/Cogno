package com.ntoprevd.cogno;

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

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------- 透明状态栏（让网页内容延伸到状态栏下方）----------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 状态栏背景完全透明
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            // 让应用内容布局全屏，不自动预留状态栏空间
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        webView = findViewById(R.id.main_webview);
        // 关键：禁止 WebView 自动预留状态栏内边距，由网页 CSS 控制顶部间距
        webView.setFitsSystemWindows(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // ---------- 注入 JavaScript 接口，供网页调用改变状态栏图标颜色 ----------
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");

        // 返回键处理：支持 WebView 内部返回
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

    /**
     * JavaScript 接口类
     * 网页可通过 window.Android.setStatusBarDarkMode(isDarkMode) 调用
     */
    private class WebAppInterface {
        /**
         * 设置状态栏图标/文字颜色
         * @param isDarkMode true: 深色模式（背景深色） -> 状态栏文字应为白色（清除 LIGHT_STATUS_BAR）
         *                   false: 浅色模式（背景浅色） -> 状态栏文字应为黑色（添加 LIGHT_STATUS_BAR）
         */
        @JavascriptInterface
        public void setStatusBarDarkMode(boolean isDarkMode) {
            // 必须切换到 UI 线程操作 View
            runOnUiThread(() -> {
                // Android 6.0+ 才支持动态改变状态栏文字颜色
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    View decorView = getWindow().getDecorView();
                    int flags = decorView.getSystemUiVisibility();
                    if (isDarkMode) {
                        // 深色模式 -> 白色文字，清除 LIGHT_STATUS_BAR 标志
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    } else {
                        // 浅色模式 -> 黑色文字，添加 LIGHT_STATUS_BAR 标志
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }
    }
}