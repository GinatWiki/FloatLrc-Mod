package com.folatkikoeru.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;



import com.github.lzyzsd.jsbridge.BridgeWebView;




import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends Activity {

    public static MyWebView mWebView;
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;
    private EditText et;
    private Button bt;
    private View bottomBar;
    private TextView subtitleText;
    private Button exitSubtitleButton;
    private Button subtitlePauseButton;
    private SeekBar subtitleSizeSeekbar;
    private SeekBar subtitleBrightnessSeekbar;
    private View subtitleTouchLayer;
    private LyricPoller lyricPoller;
    private boolean subtitleMode = false;
    private boolean subtitlePaused = false;

    /** 字幕模式控制条自动隐藏延时（5 秒） */
    private static final long CONTROLS_HIDE_DELAY_MS = 5000L;
    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!subtitleMode) {
                return;
            }
            subtitleSizeSeekbar.setVisibility(View.GONE);
            subtitleBrightnessSeekbar.setVisibility(View.GONE);
            exitSubtitleButton.setVisibility(View.GONE);
            subtitlePauseButton.setVisibility(View.GONE);
        }
    };

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt = (Button) findViewById(R.id.button);
        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();

//        mWebView.onPause({
//                @Override
//
//        });
        webSettings.setBlockNetworkImage(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSafeBrowsingEnabled(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setAllowContentAccess(false);


        mWebView.setWebViewClient(new MyWebViewClient());
        bottomBar = findViewById(R.id.bottom_bar);
        subtitleText = findViewById(R.id.subtitle_text);
        exitSubtitleButton = findViewById(R.id.btn_exit_subtitle);
        subtitleSizeSeekbar = findViewById(R.id.subtitle_size_seekbar);
        int subtitleSize = getSubtitleSize();
        subtitleSizeSeekbar.setProgress(subtitleSize);
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize);
        subtitleSizeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, progress);
                // 拖动期间重置自动隐藏倒计时，避免拉条中途消失
                if (subtitleMode) {
                    scheduleControlsHide();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = getSharedPreferences("SubtitleSize", MODE_PRIVATE).edit();
                editor.putInt("SubtitleSize", seekBar.getProgress());
                editor.commit();
            }
        });
        subtitleTouchLayer = findViewById(R.id.subtitle_touch_layer);
        subtitleTouchLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubtitleControls();
            }
        });
        subtitlePauseButton = findViewById(R.id.btn_subtitle_pause);
        updatePauseButton();
        subtitleBrightnessSeekbar = findViewById(R.id.subtitle_brightness_seekbar);
        // 竖向 SeekBar（rotation=270）：轨道沿宽度方向，视觉长度 = 布局宽度；
        // 旋转绕视图中心，宽度加大后视觉条会被推向右侧，故左移 (宽-厚)/2 使其回到屏幕左缘
        int barLength = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        int thickness = (int) (48 * getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams brightnessLp = subtitleBrightnessSeekbar.getLayoutParams();
        brightnessLp.width = barLength;
        brightnessLp.height = thickness;
        subtitleBrightnessSeekbar.setLayoutParams(brightnessLp);
        subtitleBrightnessSeekbar.setTranslationX(-(barLength - thickness) / 2f);
        int subtitleBrightness = getSubtitleBrightness();
        subtitleBrightnessSeekbar.setProgress(subtitleBrightness);
        applySubtitleBrightness(subtitleBrightness);
        subtitleBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                applySubtitleBrightness(progress);
                // 拖动期间重置自动隐藏倒计时，避免拉条中途消失
                if (subtitleMode) {
                    scheduleControlsHide();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = getSharedPreferences("SubtitleBrightness", MODE_PRIVATE).edit();
                editor.putInt("SubtitleBrightness", seekBar.getProgress());
                editor.commit();
            }
        });
        lyricPoller = new LyricPoller(mWebView, new LyricPoller.Listener() {
            @Override
            public void onLyric(String lyric) {
                onLyricUpdate(lyric);
            }
        });
        SharedPreferences UrlInfo = getSharedPreferences("Url", MODE_PRIVATE);
        final String[] Url = {UrlInfo.getString("Url", null)};
        if (Url[0] == null){
            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("输入URL").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("Cancel", null);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    Url[0] = inputServer.getText().toString();
                    SharedPreferences.Editor editor = UrlInfo.edit();
                    editor.putString("Url", Url[0]);
                    editor.commit();
                    mWebView.loadUrl(Url[0]);
                }
            });
            builder.show();
        }else{
            mWebView.loadUrl(Url[0]);
        }
        // REMOTE RESOURCE
        EditText editText=(EditText)findViewById(R.id.editTextNumber2);
        SharedPreferences SizeInfo = getSharedPreferences("Size", MODE_PRIVATE);
        final String[] Size = {SizeInfo.getString("Size", null)};
        if (Size[0] == null){
            Size[0] = editText.getText().toString();
            SharedPreferences.Editor editor = SizeInfo.edit();
            editor.putString("Size", Size[0]);
            editor.commit();
        }else{
            editText.setText(Size[0]);
        }

//        mWebView.setOnTouchListener(new FloatingOnTouchListener());



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
    public void onPause() {
        super.onPause();
    }







    public void startFloatingButtonService(View view) {
        EditText editText=(EditText)findViewById(R.id.editTextNumber2);

        //Intent Lrc = new Intent(MainActivity.this, FloatingButtonService.class);
        if (FloatingButtonService.isStarted) {
            stopService(new Intent(this, FloatingButtonService.class));
            FloatingButtonService.isStarted = false;
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            startService(new Intent(this, FloatingButtonService.class));
            FloatingButtonService.isStarted = true;
            SharedPreferences SizeInfo = getSharedPreferences("Size", MODE_PRIVATE);
            String[] Size = {SizeInfo.getString("Size", null)};
            Size[0] = editText.getText().toString();
            SharedPreferences.Editor editor = SizeInfo.edit();
            editor.putString("Size", Size[0]);
            editor.commit();
        }


    }

        public void startFloatingWebViewService(View view){
            //Intent Lrc = new Intent(MainActivity.this, FloatingButtonService.class);
            if (FloatingWebViewService.isStarted) {
                stopService(new Intent(this, FloatingWebViewService.class));
                FloatingWebViewService.isStarted = false;
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
            } else {
                startService(new Intent(this, FloatingWebViewService.class));
                FloatingWebViewService.isStarted = true;
            }
        }



    // ==================== 字幕播放模式 ====================

    public void startSubtitleMode(View view) {
        if (subtitleMode) {
            return;
        }
        subtitleMode = true;
        // 进入字幕播放：开启防息屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bottomBar.setVisibility(View.GONE);
        mWebView.setVisibility(View.GONE);
        subtitleText.setVisibility(View.GONE);
        subtitleTouchLayer.setVisibility(View.VISIBLE);
        subtitlePaused = false;
        updatePauseButton();
        exitSubtitleButton.setVisibility(View.VISIBLE);
        subtitlePauseButton.setVisibility(View.VISIBLE);
        subtitleSizeSeekbar.setVisibility(View.VISIBLE);
        subtitleBrightnessSeekbar.setVisibility(View.VISIBLE);
        enterImmersive();
        hidePageLyricElement();
        lyricPoller.start();
        // 5 秒后自动隐藏控制条，点击屏幕唤出
        scheduleControlsHide();
    }

    public void exitSubtitleMode(View view) {
        exitSubtitleMode();
    }

    private void exitSubtitleMode() {
        if (!subtitleMode) {
            return;
        }
        subtitleMode = false;
        // 退出字幕播放：恢复系统默认息屏；若仍处于暂停状态则恢复网页音乐
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (subtitlePaused) {
            resumeWebAudio();
        }
        subtitlePaused = false;
        controlsHandler.removeCallbacks(hideControlsRunnable);
        lyricPoller.stop();
        restorePageLyricElement();
        exitImmersive();
        subtitleText.setVisibility(View.GONE);
        exitSubtitleButton.setVisibility(View.GONE);
        subtitlePauseButton.setVisibility(View.GONE);
        subtitleSizeSeekbar.setVisibility(View.GONE);
        subtitleBrightnessSeekbar.setVisibility(View.GONE);
        subtitleTouchLayer.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
    }

    /** 重置并安排控制条 5 秒后自动隐藏 */
    private void scheduleControlsHide() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        controlsHandler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS);
    }

    /** 唤出字幕模式控制条并重新计时 */
    private void showSubtitleControls() {
        if (!subtitleMode) {
            return;
        }
        exitSubtitleButton.setVisibility(View.VISIBLE);
        subtitlePauseButton.setVisibility(View.VISIBLE);
        subtitleSizeSeekbar.setVisibility(View.VISIBLE);
        subtitleBrightnessSeekbar.setVisibility(View.VISIBLE);
        scheduleControlsHide();
    }

    // ==================== 字幕播放/暂停 ====================

    /** 播放/暂停切换：暂停时网页音乐停止、字幕隐藏 */
    public void toggleSubtitlePause(View view) {
        if (!subtitleMode) {
            return;
        }
        subtitlePaused = !subtitlePaused;
        updatePauseButton();
        if (subtitlePaused) {
            pauseWebAudio();
            subtitleText.setVisibility(View.GONE);
        } else {
            resumeWebAudio();
        }
        scheduleControlsHide();
    }

    private void updatePauseButton() {
        if (subtitlePauseButton == null) {
            return;
        }
        subtitlePauseButton.setText(subtitlePaused ? R.string.subtitle_resume_symbol : R.string.subtitle_pause_symbol);
    }

    /** 暂停网页音乐：记录正在播放的 audio/video 元素并全部暂停 */
    private void pauseWebAudio() {
        if (mWebView == null) {
            return;
        }
        try {
            mWebView.evaluateJavascript(
                    "javascript:(function(){"
                            + "if(window.__fk_paused){return 0;}"
                            + "window.__fk_paused=[];"
                            + "var els=document.querySelectorAll('audio,video');"
                            + "for(var i=0;i<els.length;i++){if(!els[i].paused){window.__fk_paused.push(els[i]);els[i].pause();}}"
                            + "return window.__fk_paused.length;"
                            + "})()",
                    null);
        } catch (Exception ignored) {
        }
    }

    /** 恢复网页音乐：仅恢复此前由本应用暂停的元素 */
    private void resumeWebAudio() {
        if (mWebView == null) {
            return;
        }
        try {
            mWebView.evaluateJavascript(
                    "javascript:(function(){"
                            + "if(!window.__fk_paused){return 0;}"
                            + "var saved=window.__fk_paused;window.__fk_paused=null;"
                            + "for(var i=0;i<saved.length;i++){var p=saved[i].play();if(p&&p.then){p.then(function(){}).catch(function(){});}}"
                            + "return saved.length;"
                            + "})()",
                    null);
        } catch (Exception ignored) {
        }
    }

    /** 歌词轮询回调（主线程）：空歌词或已暂停时隐藏字幕层；清理前导/尾部空白防止居中偏移 */
    private void onLyricUpdate(String lyric) {
        if (subtitlePaused) {
            subtitleText.setVisibility(View.GONE);
            return;
        }
        String clean = lyric == null ? null : lyric.replace('\u00A0', ' ').trim();
        if (clean == null || clean.isEmpty()) {
            subtitleText.setVisibility(View.GONE);
            return;
        }
        subtitleText.setVisibility(View.VISIBLE);
        subtitleText.setText(clean);
        subtitleText.setTextColor(getLyricColor());
    }

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void exitImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    /** 字幕模式下隐藏页面自身的歌词区，避免与字幕层重复显示（best-effort） */
    private void hidePageLyricElement() {
        if (mWebView == null) {
            return;
        }
        try {
            mWebView.evaluateJavascript(
                    "javascript:(function(){var el=document.getElementById('lyric');if(el){window.__fk_lyric_display=el.style.display;el.style.display='none';}})()",
                    null);
        } catch (Exception ignored) {
        }
    }

    /** 退出字幕模式时恢复页面歌词区原始 display */
    private void restorePageLyricElement() {
        if (mWebView == null) {
            return;
        }
        try {
            mWebView.evaluateJavascript(
                    "javascript:(function(){var el=document.getElementById('lyric');if(el){el.style.display=window.__fk_lyric_display||'';}})()",
                    null);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        if (subtitleMode) {
            exitSubtitleMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        if (lyricPoller != null) {
            lyricPoller.stop();
        }
        super.onDestroy();
    }

    // ==================== 悬浮字体颜色设置 ====================

    /** 当前歌词颜色（SharedPreferences "Color"，默认 #bb00ff） */
    private int getLyricColor() {
        SharedPreferences colorInfo = getSharedPreferences("Color", MODE_PRIVATE);
        return colorInfo.getInt("Color", Color.parseColor("#bb00ff"));
    }

    /** 字幕模式字号（SharedPreferences "SubtitleSize"，默认 48sp，范围 24-100） */
    private int getSubtitleSize() {
        return getSharedPreferences("SubtitleSize", MODE_PRIVATE).getInt("SubtitleSize", 48);
    }

    /** 字幕亮度（SharedPreferences "SubtitleBrightness"，默认 100%，范围 20-100） */
    private int getSubtitleBrightness() {
        return getSharedPreferences("SubtitleBrightness", MODE_PRIVATE).getInt("SubtitleBrightness", 100);
    }

    /** 将亮度百分比应用到字幕文本（黑底上降低视图 alpha 即调暗，防夜间刺眼） */
    private void applySubtitleBrightness(int percent) {
        subtitleText.setAlpha(percent / 100f);
    }

    /** 色彩轮盘弹窗：选择悬浮歌词颜色并持久化 */
    public void showColorPickerDialog(View view) {
        View content = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        final ColorWheelView colorWheel = content.findViewById(R.id.color_wheel);
        final TextView preview = content.findViewById(R.id.color_preview_text);
        final int[] selected = {getLyricColor()};
        colorWheel.setColor(selected[0]);
        preview.setTextColor(selected[0]);
        colorWheel.setOnColorChangedListener(new ColorWheelView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                selected[0] = color;
                preview.setTextColor(color);
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.button_color)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = getSharedPreferences("Color", MODE_PRIVATE).edit();
                        editor.putInt("Color", selected[0]);
                        editor.commit();
                        Toast.makeText(MainActivity.this, R.string.color_saved, Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();
    }

}
