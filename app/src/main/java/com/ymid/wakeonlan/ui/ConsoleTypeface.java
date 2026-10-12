package com.ymid.wakeonlan.ui;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.textfield.TextInputLayout;

import com.ymid.wakeonlan.R;

/** Applies the console font where Material ignores the theme, such as text field labels. */
public final class ConsoleTypeface {

    private ConsoleTypeface() {
    }

    public static void applyToTextInputs(View root) {
        Typeface typeface = ResourcesCompat.getFont(root.getContext(), R.font.jetbrains_mono);
        if (typeface != null) {
            apply(root, typeface);
        }
    }

    private static void apply(View view, Typeface typeface) {
        if (view instanceof TextInputLayout) {
            ((TextInputLayout) view).setTypeface(typeface);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), typeface);
            }
        }
    }
}
