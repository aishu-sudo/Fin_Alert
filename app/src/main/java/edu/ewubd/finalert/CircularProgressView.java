package edu.ewubd.finalert;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private Paint bgPaint, progressPaint, centerPaint;
    private RectF rectF;
    private float progress = 1f; // 0.0 to 1.0 (remaining %)
    private boolean isDark = false;

    private static final int COLOR_TEAL     = 0xFF33D49C;
    private static final int COLOR_DARK     = 0xFF1A1A2E;
    private static final int COLOR_BG_RING  = 0x33000000;
    private static final float STROKE_WIDTH = 14f;

    public CircularProgressView(Context ctx) { super(ctx); init(); }
    public CircularProgressView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public CircularProgressView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(STROKE_WIDTH);
        bgPaint.setColor(Color.parseColor("#22000000"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(STROKE_WIDTH);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(COLOR_TEAL);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(COLOR_TEAL);

        rectF = new RectF();
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        isDark = this.progress <= 0.05f;
        centerPaint.setColor(isDark ? COLOR_DARK : COLOR_TEAL);
        progressPaint.setColor(isDark ? Color.parseColor("#FF444466") : COLOR_TEAL);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float pad = STROKE_WIDTH / 2f + 4f;
        rectF.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Fill circle
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) - STROKE_WIDTH / 2f - 4f;
        canvas.drawCircle(cx, cy, r, centerPaint);

        // Background ring
        canvas.drawArc(rectF, -90, 360, false, bgPaint);

        // Progress arc (remaining %)
        if (progress > 0f) {
            canvas.drawArc(rectF, -90, progress * 360f, false, progressPaint);
        }
    }
}

