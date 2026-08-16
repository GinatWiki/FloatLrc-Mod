package com.folatkikoeru.app;

import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.json.JSONArray;

/**
 * 每 100ms 轮询网页 {@code #lyric} 元素的 innerText，供悬浮窗与字幕模式共用。
 * 回调均发生在主线程；WebView 为空或不可用时回调 null。
 */
public class LyricPoller {

    public interface Listener {
        /** lyric 为 null 或空串表示暂无歌词/不可用。 */
        void onLyric(String lyric);
    }

    private static final long INTERVAL_MS = 100L;

    private final WebView webView;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            fetch();
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public LyricPoller(WebView webView, Listener listener) {
        this.webView = webView;
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        handler.post(tick);
    }

    public synchronized void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    private void fetch() {
        if (webView == null) {
            listener.onLyric(null);
            return;
        }
        try {
            webView.evaluateJavascript(
                    "javascript:document.getElementById(\"lyric\").innerText",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            listener.onLyric(unquote(s));
                        }
                    });
        } catch (Exception e) {
            // WebView 已销毁等场景：视为暂无歌词，轮询继续，可自愈
            listener.onLyric(null);
        }
    }

    /** 剥除 evaluateJavascript 返回值的 JSON 引号与转义 */
    private static String unquote(String s) {
        if (s == null) {
            return null;
        }
        try {
            return new JSONArray("[" + s + "]").getString(0);
        } catch (Exception e) {
            return s;
        }
    }
}
