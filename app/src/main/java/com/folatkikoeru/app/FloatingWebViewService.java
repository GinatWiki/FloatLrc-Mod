package com.folatkikoeru.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import org.jetbrains.annotations.Nullable;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;

public class FloatingWebViewService extends Service {
    public static boolean isStarted = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private Button button;
    public static WebView mWebView;
    boolean isClosed = false;




    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = 1920;
        layoutParams.height = 1080;
        layoutParams.x = 0;
        layoutParams.y = 0;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            Notification.Builder builder = new Notification.Builder(this);
//            builder.setSmallIcon(R.mipmap.ic_launcher);
//            builder.setContentTitle("KeepAppAlive");
//            builder.setContentText("DaemonService is runing...");
//            startForeground(100, builder.build());
//        } else {
//            startForeground(100, new Notification());
//        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
//        MainActivity.mWebView.onPause();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                MainActivity.mWebView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Thread.sleep(0);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        MainActivity.mWebView.onResume();
//                        MainActivity.mWebView.resumeTimers();
//                        MainActivity.mWebView.evaluateJavascript("javascript:document.getElementById(\"lyric\").innerText\n", new ValueCallback<String>() {
//                            @Override
//                            public void onReceiveValue(String s) {
//                                button.setText(s);
//                                Log.d("d", s);
//                                button.refreshDrawableState();
//                                button.setOnTouchListener(new FloatingOnTouchListener());
//                            }
//                        });
//                    }
//                });
//            }
//        };
//        Timer timer = new Timer();
//        timer.schedule(task, 0, 100);

        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
//            MainActivity.mWebView.post(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(0);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    MainActivity.mWebView.evaluateJavascript("javascript:document.getElementById(\"lyric\").innerText\n", new ValueCallback<String>() {
//                        @Override
//                        public void onReceiveValue(String s) {
//                            button.setText(s);
//                        }
//                    });
//                }
//            });
//            button = new Button(getApplicationContext());
//            if (button.getText().equals("")){
//                button.setText("SBSZZ");
//            }
//
//            button.setTextColor(Color.RED);
//            button.setBackgroundColor(Color.argb(0,79,79,79));


//        mWebView.onPause({
//                @Override
//
//        });
//            webSettings.setBlockNetworkImage(false);
//            webSettings.setJavaScriptEnabled(true);
//            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//            webSettings.setAllowFileAccess(true);
//            webSettings.setDomStorageEnabled(true);
//            webSettings.setSafeBrowsingEnabled(false);
//            webSettings.setBlockNetworkLoads(false);
//            webSettings.setAllowContentAccess(false);



            mWebView = new WebView(this);

            mWebView.getSettings().setJavaScriptEnabled(true); //设置允许Js

            /*设置webview控件背景透明*/

            mWebView.setBackgroundColor(Color.TRANSPARENT);

            mWebView.setWebChromeClient(new WebChromeClient());

            // REMOTE RESOURCE
            mWebView.loadUrl("");
            windowManager.addView(mWebView, layoutParams);

            mWebView.setOnTouchListener(new FloatingOnTouchListener());
        }
    }
    private void destroyFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            windowManager.removeView(mWebView);
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        destroyFloatingWindow();
        super.onDestroy();
    }
}
