package com.folatkikoeru.app;

import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.json.JSONArray;

/**
 * 姣?100ms 杞缃戦〉 {@code #lyric} 鍏冪礌鐨?innerText锛屼緵鎮诞绐椾笌瀛楀箷妯″紡鍏辩敤銆?
 * 鍥炶皟鍧囧彂鐢熷湪涓荤嚎绋嬶紱WebView 涓虹┖鎴栦笉鍙敤鏃跺洖璋?null銆?
 */
public class LyricPoller {

    public interface Listener {
        /** lyric 涓?null 鎴栫┖涓茶〃绀烘殏鏃犳瓕璇?涓嶅彲鐢ㄣ€?*/
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
            // WebView 宸查攢姣佺瓑鍦烘櫙锛氳涓烘殏鏃犳瓕璇嶏紝杞缁х画锛屽彲鑷剤
            listener.onLyric(null);
        }
    }

    /** 鍓ラ櫎 evaluateJavascript 杩斿洖鍊肩殑 JSON 寮曞彿涓庤浆涔?*/
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
