package com.folatkikoeru.app;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;


public class FloatingButtonService extends Service {
    public static boolean isStarted = false;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private Button button;
    private LyricPoller lyricPoller;
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
        lyricPoller = new LyricPoller(MainActivity.mWebView, new LyricPoller.Listener() {
            @Override
            public void onLyric(String lyric) {
                applyLyric(lyric);
            }
        });
        lyricPoller.start();

        return super.onStartCommand(intent, flags, startId);
    }

    /** 歌词轮询回调（主线程）：按偏好颜色/字号渲染到悬浮按钮 */
    private void applyLyric(String lyric) {
        if (button == null) {
            return;
        }
        String text = (lyric == null) ? "" : lyric;
        SpannableString ss1 = new SpannableString(text);
        if (ss1.length() > 0) {
            ss1.setSpan(new ForegroundColorSpan(readColor()), 0, ss1.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ss1.setSpan(new AbsoluteSizeSpan(readSize(), true), 0, ss1.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ss1.setSpan(new StyleSpan(Typeface.BOLD), 0, ss1.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        button.setText(ss1);
    }

    /** 悬浮歌词颜色（SharedPreferences "Color"，默认 #bb00ff，每 tick 读取以即时生效） */
    private int readColor() {
        SharedPreferences colorInfo = getSharedPreferences("Color", MODE_PRIVATE);
        return colorInfo.getInt("Color", Color.parseColor("#bb00ff"));
    }

    /** 悬浮歌词字号（SharedPreferences "Size"，解析失败回退 15） */
    private int readSize() {
        try {
            String s = getSharedPreferences("Size", MODE_PRIVATE).getString("Size", "15");
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 15;
        }
    }


    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            button = new Button(getApplicationContext());
            if (button.getText().equals("")){
                button.setText("SBSZZ");
            }

//            button.setTextColor(Color.RED);

            SpannableString ss1 = new SpannableString("");
            ss1.setSpan(new AbsoluteSizeSpan(readSize(), true), 0, 0,
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
        if (lyricPoller != null) {
            lyricPoller.stop();
        }
        destroyFloatingWindow();
        super.onDestroy();
    }
}
