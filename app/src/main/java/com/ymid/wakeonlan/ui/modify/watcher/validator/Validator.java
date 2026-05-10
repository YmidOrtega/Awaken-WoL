package com.ymid.wakeonlan.ui.modify.watcher.validator;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.ymid.wakeonlan.ui.ObfuscatedEditText;

public abstract class Validator implements TextWatcher {

    private final EditText editTextView;

    protected Validator(EditText editTextView) {
        this.editTextView = editTextView;
    }

    abstract ValidationResult validate(String text);

    abstract int getErrorMessageStringId();

    protected String getErrorMessage() {
        return editTextView.getContext().getString(getErrorMessageStringId());
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // Get real text value for ObfuscatedEditText, otherwise use getText()
        String textToValidate;
        if (editTextView instanceof ObfuscatedEditText) {
            textToValidate = ((ObfuscatedEditText) editTextView).getRealTextValue();
        } else {
            textToValidate = editTextView.getText() != null ? editTextView.getText().toString() : "";
        }

        ValidationResult validate = validate(textToValidate);
        if (validate == ValidationResult.VALID) {
            editTextView.setError(null);
        } else {
            editTextView.setError(getErrorMessage());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing
    }
}
