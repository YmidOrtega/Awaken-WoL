package com.ymid.wakeonlan.ui.list;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import com.ymid.wakeonlan.R;

/** Latency sparkline: a thin line with a faint fill, or a dashed baseline when there is no data. */
public class SparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private List<Long> values = new ArrayList<>();

    public SparklineView(Context context) {
        this(context, null);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f * density);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        fillPaint.setStyle(Paint.Style.FILL);

        baselinePaint.setStyle(Paint.Style.STROKE);
        baselinePaint.setStrokeWidth(density);
        baselinePaint.setColor(ContextCompat.getColor(context, R.color.consoleLineColor));
        baselinePaint.setPathEffect(new DashPathEffect(new float[]{2 * density, 3 * density}, 0));

        setColor(ContextCompat.getColor(context, R.color.ledOnlineColor));
    }

    public void setColor(@ColorInt int color) {
        linePaint.setColor(color);
        fillPaint.setColor(color);
        fillPaint.setAlpha(31);
        invalidate();
    }

    public void setValues(List<Long> values) {
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        float left = getPaddingLeft();
        float top = getPaddingTop();
        float bottom = top + h;

        if (values.size() < 2) {
            canvas.drawLine(left, bottom - 1, left + w, bottom - 1, baselinePaint);
            return;
        }

        long max = 1;
        for (Long v : values) {
            max = Math.max(max, v);
        }
        // Headroom keeps the line off the top edge
        float scale = (h - linePaint.getStrokeWidth()) / (max * 1.3f);
        float step = w / (values.size() - 1);

        linePath.reset();
        fillPath.reset();
        fillPath.moveTo(left, bottom);
        for (int i = 0; i < values.size(); i++) {
            float x = left + i * step;
            float y = bottom - values.get(i) * scale;
            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
            fillPath.lineTo(x, y);
        }
        fillPath.lineTo(left + w, bottom);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
