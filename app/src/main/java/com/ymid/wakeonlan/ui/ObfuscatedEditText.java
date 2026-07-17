package com.ymid.wakeonlan.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;

public class ObfuscatedEditText extends TextInputEditText {

    private static final String TAG = "ObfuscatedEditText";
    private String realText = "";
    private boolean isRevealed = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable;
    private boolean isUpdatingText = false;

    public ObfuscatedEditText(Context context) {
        super(context);
        init();
    }

    public ObfuscatedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ObfuscatedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnFocusChangeListener((v, hasFocus) -> {
            try {
                if (hasFocus) {
                    reveal();
                } else {
                    hideImmediately();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in focus change", e);
            }
        });

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    if (!isUpdatingText && isRevealed) {
                        realText = s != null ? s.toString() : "";
                        scheduleHide();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in text change", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public Editable getText() {
        return super.getText();
    }

    public String getRealTextValue() {
        if (isRevealed) {
            Editable text = super.getText();
            return text != null ? text.toString() : "";
        }
        return realText;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        try {
            if (!isUpdatingText) {
                this.realText = text == null ? "" : text.toString();
            }
            if (!isUpdatingText) {
                if (!isRevealed && this.realText.length() > 0) {
                    isUpdatingText = true;
                    try {
                        super.setText(obfuscate(this.realText), type);
                    } finally {
                        isUpdatingText = false;
                    }
                } else {
                    super.setText(text, type);
                }
            } else {
                super.setText(text, type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setText", e);
        }
    }

    public void setRealText(String text) {
        this.realText = text == null ? "" : text;
        isUpdatingText = true;
        try {
            if (isRevealed) {
                super.setText(realText, BufferType.EDITABLE);
            } else if (realText.length() > 0) {
                super.setText(obfuscate(realText), BufferType.EDITABLE);
            } else {
                super.setText("", BufferType.EDITABLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setRealText", e);
        } finally {
            isUpdatingText = false;
        }
    }

    public String getRealText() {
        return realText;
    }

    private void reveal() {
        if (isRevealed) {
            scheduleHide();
            return;
        }

        isRevealed = true;
        isUpdatingText = true;
        try {
            super.setText(realText, BufferType.EDITABLE);
            post(() -> {
                try {
                    Editable text = getText();
                    if (text != null && text.length() > 0) {
                        setSelection(Math.min(text.length(), text.length()));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting selection", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in reveal", e);
        } finally {
            isUpdatingText = false;
        }
        scheduleHide();
    }

    private void scheduleHide() {
        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
        }

        hideRunnable = () -> {
            if (!hasFocus()) {
                hideImmediately();
            }
        };

        handler.postDelayed(hideRunnable, 5000);
    }

    private void hideImmediately() {
        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
        }

        if (!isRevealed) {
            return;
        }

        isRevealed = false;
        try {
            CharSequence currentText = super.getText();
            realText = currentText != null ? currentText.toString() : "";
            updateObfuscatedDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Error in hideImmediately", e);
        }
    }

    private void updateObfuscatedDisplay() {
        isUpdatingText = true;
        try {
            super.setText(obfuscate(realText), BufferType.EDITABLE);
        } catch (Exception e) {
            Log.e(TAG, "Error in updateObfuscatedDisplay", e);
        } finally {
            isUpdatingText = false;
        }
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
        }
    }
}
