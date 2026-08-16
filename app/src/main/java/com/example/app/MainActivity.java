package com.example.app;

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
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
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
            FloatingButtonService.size = Integer.parseInt(editText.getText().toString());
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



}
