package com.folatkikoeru.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 鑷粯 HSV 鑹插僵杞洏锛氳搴?鑹茬浉锛坔ue锛夛紝鍗婂緞=楗卞拰搴︼紙saturation锛夛紝浜害鍥哄畾涓?1銆?
 * 涓績鍦嗗唴鏄剧ず褰撳墠閫変腑棰滆壊锛岀偣鍑?鎷栧姩鑹茬幆閫夊彇棰滆壊銆?
 */
public class ColorWheelView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    /** 涓績绌虹櫧锛堥€変腑鑹查瑙堬級鍗婂緞鍗犳暣涓疆鐩樺崐寰勭殑姣斾緥 */
    private static final float INNER_RADIUS_FRACTION = 0.28f;

    private int selectedColor = Color.parseColor("#bb00ff");
    private final float[] hsv = new float[]{0f, 0f, 1f};
    private Bitmap wheelBitmap;
    private final Paint indicatorFillPaint;
    private final Paint indicatorStrokePaint;
    private final Paint borderPaint;
    private OnColorChangedListener listener;

    public ColorWheelView(Context context) {
        this(context, null);
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        indicatorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorFillPaint.setStyle(Style.FILL);
        indicatorFillPaint.setColor(Color.WHITE);

        indicatorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorStrokePaint.setStyle(Style.STROKE);
        indicatorStrokePaint.setStrokeWidth(dp(1.5f));
        indicatorStrokePaint.setColor(0x66000000);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Style.STROKE);
        borderPaint.setStrokeWidth(dp(1f));
        borderPaint.setColor(0x33000000);
    }

    public void setOnColorChangedListener(OnColorChangedListener l) {
        this.listener = l;
    }

    /** 璁剧疆褰撳墠閫変腑棰滆壊锛堝悓鏃舵洿鏂版寚绀虹偣浣嶇疆锛?*/
    public void setColor(int color) {
        selectedColor = color;
        Color.colorToHSV(color, hsv);
        invalidate();
    }

    /** 褰撳墠閫変腑棰滆壊 */
    public int getColor() {
        return selectedColor;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            buildWheelBitmap(Math.min(w, h));
        }
    }

    /** 閫愬儚绱犵敓鎴愯壊鐜綅鍥撅紝size 鍙樺寲鏃堕噸寤?*/
    private void buildWheelBitmap(int size) {
        wheelBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        float cx = size / 2f;
        float cy = size / 2f;
        float outerR = size / 2f;
        float innerR = outerR * INNER_RADIUS_FRACTION;
        int[] pixels = new int[size * size];
        float[] hsvLocal = new float[]{0f, 0f, 1f};
        for (int y = 0; y < size; y++) {
            float dy = y - cy;
            for (int x = 0; x < size; x++) {
                float dx = x - cx;
                float r = (float) Math.sqrt(dx * dx + dy * dy);
                if (r < innerR || r > outerR) {
                    pixels[y * size + x] = Color.TRANSPARENT;
                    continue;
                }
                // hue锛?掳 鍦ㄦ鍙虫柟锛岄『鏃堕拡锛泂at锛氬唴鍦?0 鈫?澶栧湀 1
                float hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360f) % 360f);
                float sat = (r - innerR) / (outerR - innerR);
                hsvLocal[0] = hue;
                hsvLocal[1] = Math.min(1f, sat);
                hsvLocal[2] = 1f;
                pixels[y * size + x] = Color.HSVToColor(hsvLocal);
            }
        }
        wheelBitmap.setPixels(pixels, 0, size, 0, 0, size, size);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (wheelBitmap == null) {
            return;
        }
        int size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        float cx = left + size / 2f;
        float cy = top + size / 2f;
        float outerR = size / 2f;
        float innerR = outerR * INNER_RADIUS_FRACTION;

        canvas.drawBitmap(wheelBitmap, left, top, null);

        // 涓績鍦嗗～鍏呭綋鍓嶉€変腑鑹诧紙棰勮锛?
        Paint centerPaint = indicatorFillPaint;
        centerPaint.setColor(selectedColor);
        canvas.drawCircle(cx, cy, innerR - dp(3f), centerPaint);
        canvas.drawCircle(cx, cy, innerR - dp(3f), borderPaint);

        // 澶栧湀鎻忚竟
        canvas.drawCircle(cx, cy, outerR - borderPaint.getStrokeWidth() / 2f, borderPaint);

        // 閫変腑鎸囩ず鐐?
        float indicatorR = innerR + (outerR - innerR) * Math.min(1f, hsv[1]);
        float angle = (float) Math.toRadians(hsv[0]);
        float ix = cx + (float) (indicatorR * Math.cos(angle));
        float iy = cy + (float) (indicatorR * Math.sin(angle));
        indicatorFillPaint.setColor(Color.WHITE);
        canvas.drawCircle(ix, iy, dp(6f), indicatorFillPaint);
        canvas.drawCircle(ix, iy, dp(6f), indicatorStrokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            int size = Math.min(getWidth(), getHeight());
            if (size <= 0) {
                return true;
            }
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float dx = event.getX() - cx;
            float dy = event.getY() - cy;
            float outerR = size / 2f;
            float innerR = outerR * INNER_RADIUS_FRACTION;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            if (r <= outerR) {
                float clampedR = Math.max(innerR, r);
                float hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360f) % 360f);
                float sat = (clampedR - innerR) / (outerR - innerR);
                hsv[0] = hue;
                hsv[1] = Math.min(1f, sat);
                hsv[2] = 1f;
                selectedColor = Color.HSVToColor(hsv);
                invalidate();
                if (listener != null) {
                    listener.onColorChanged(selectedColor);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
