package com.fatima.unsaid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int percent = 0;

    public RingView(Context context) {
        super(context);
        init();
    }

    public RingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float stroke = 10 * getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(0xFF3A323A);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(ContextCompat.getColor(getContext(), R.color.unsaid_accent));
    }

    public void setPercent(int p) {
        percent = Math.max(0, Math.min(100, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float half = track.getStrokeWidth() / 2f;
        box.set(half, half, getWidth() - half, getHeight() - half);
        canvas.drawArc(box, 0, 360, false, track);
        if (percent > 0) {
            canvas.drawArc(box, -90, 360f * percent / 100f, false, arc);
        }
    }
}