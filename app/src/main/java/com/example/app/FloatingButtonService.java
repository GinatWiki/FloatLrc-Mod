package com.example.app;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import org.jetbrains.annotations.Nullable;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;


public class FloatingButtonService extends Service {
    public static boolean isStarted = false;
    public static int size = 15;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private Button button;
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
        layoutParams.width = 1000;
        layoutParams.height = 400;
        layoutParams.x = 300;
        layoutParams.y = 300;
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
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                MainActivity.mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MainActivity.mWebView.evaluateJavascript("javascript:document.getElementById(\"lyric\").innerText\n", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                SpannableString ss1 = new SpannableString(s);
                                ss1.setSpan(new ForegroundColorSpan(Color.parseColor("#bb00ff")), 0, s.length(),
                                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                ss1.setSpan(new AbsoluteSizeSpan(size,true), 0, s.length(),
                                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                ss1.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(),
                                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                button.setText(ss1);

//                                button.refreshDrawableState();
//                                button.setOnTouchListener(new FloatingOnTouchListener());
                            }
                        });
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0, 100);

        return super.onStartCommand(intent, flags, startId);
    }


    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            MainActivity.mWebView.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MainActivity.mWebView.evaluateJavascript("javascript:document.getElementById(\"lyric\").innerText\n", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            button.setText(s);
                        }
                    });
                }
            });
            button = new Button(getApplicationContext());
            if (button.getText().equals("")){
                button.setText("SBSZZ");
            }

//            button.setTextColor(Color.RED);

            SpannableString ss1 = new SpannableString("");
            ss1.setSpan(new AbsoluteSizeSpan(size, true), 0, 0,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(ss1);
            button.setBackgroundColor(Color.argb(0,79,79,79));
            windowManager.addView(button, layoutParams);

            button.setOnTouchListener(new FloatingOnTouchListener());
        }
    }
    private void destroyFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            windowManager.removeView(button);
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
