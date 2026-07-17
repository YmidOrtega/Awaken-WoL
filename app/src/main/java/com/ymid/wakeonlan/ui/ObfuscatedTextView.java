package com.ymid.wakeonlan.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.ymid.wakeonlan.R;

public class ObfuscatedTextView extends AppCompatTextView {

    private String obfuscatedText = "";
    private ValueAnimator shimmerAnimator;
    private float shimmerTranslate = 0f;
    private Paint shimmerPaint;
    private Matrix shimmerMatrix;

    public ObfuscatedTextView(Context context) {
        super(context);
        init();
    }

    public ObfuscatedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ObfuscatedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        shimmerMatrix = new Matrix();
        shimmerPaint = new Paint();
    }

    public void setObfuscatedText(String text) {
        this.obfuscatedText = obfuscate(text);
        setText(this.obfuscatedText);
        startShimmerAnimation();
    }

    private String obfuscate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (text.contains(":")) {
            String[] parts = text.split(":");
            if (parts.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) sb.append(":");
                    sb.append("••");
                }
                sb.append(":").append(parts[parts.length - 1]);
                return sb.toString();
            }
        } else if (text.contains(".")) {
            String[] parts = text.split("\\.");
            if (parts.length == 4) {
                return "•••.•••.•••." + parts[3];
            }
        }

        if (text.length() > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length() - 2; i++) {
                sb.append("•");
            }
            sb.append(text.substring(text.length() - 2));
            return sb.toString();
        }

        return "••••";
    }

    private void startShimmerAnimation() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
        }
    }

    private int interpolateColor(int colorA, int colorB, float fraction) {
        int alphaA = (colorA >> 24) & 0xff;
        int redA = (colorA >> 16) & 0xff;
        int greenA = (colorA >> 8) & 0xff;
        int blueA = colorA & 0xff;

        int alphaB = (colorB >> 24) & 0xff;
        int redB = (colorB >> 16) & 0xff;
        int greenB = (colorB >> 8) & 0xff;
        int blueB = colorB & 0xff;

        return ((int) (alphaA + (alphaB - alphaA) * fraction) << 24) |
               ((int) (redA + (redB - redA) * fraction) << 16) |
               ((int) (greenA + (greenB - greenA) * fraction) << 8) |
               (int) (blueA + (blueB - blueA) * fraction);
    }
}
