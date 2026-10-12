package com.ymid.wakeonlan.ui.scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ymid.wakeonlan.R;

/** Progress shown as a row of discrete blocks, like a dot-matrix meter. */
public class SegmentBarView extends View {

    private static final int SEGMENTS = 32;

    private final Paint filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF segment = new RectF();
    private final float gap;
    private final float radius;
    private float progress;

    public SegmentBarView(Context context) {
        this(context, null);
    }

    public SegmentBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        gap = 3 * density;
        radius = density;
        filledPaint.setColor(ContextCompat.getColor(context, R.color.ledOnlineColor));
        emptyPaint.setColor(ContextCompat.getColor(context, R.color.consoleLineColor));
    }

    /** @param progress from 0 to 1 */
    public void setProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (clamped != this.progress) {
            this.progress = clamped;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float segmentWidth = (width - gap * (SEGMENTS - 1)) / SEGMENTS;
        int filled = Math.round(progress * SEGMENTS);
        float top = getPaddingTop();
        float bottom = getHeight() - getPaddingBottom();

        for (int i = 0; i < SEGMENTS; i++) {
            float left = getPaddingLeft() + i * (segmentWidth + gap);
            segment.set(left, top, left + segmentWidth, bottom);
            canvas.drawRoundRect(segment, radius, radius, i < filled ? filledPaint : emptyPaint);
        }
    }
}
