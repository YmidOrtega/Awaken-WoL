package com.ymid.wakeonlan.ui.modify.watcher.autocomplete;

import android.text.Editable;
import android.text.TextWatcher;

public class MacAddressAutocomplete implements TextWatcher {

    boolean isDeleting = false;
    boolean shouldAppendColon = false;
    boolean shouldPrependColon = false;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        isDeleting = after == 0 && count >= 0;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() >= 17 || s.toString().endsWith(":")) {
            shouldAppendColon = false;
            return;
        }

        String[] byteSplit = s.toString().split(":");
        if (byteSplit.length == 0) {
            shouldAppendColon = false;
            return;
        }

        String lastByteSplit = byteSplit[byteSplit.length - 1];

        if (lastByteSplit != null && lastByteSplit.length() == 3) {
            shouldPrependColon = true;
            return;
        }

        shouldAppendColon = lastByteSplit != null && lastByteSplit.length() == 2;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (shouldPrependColon && !isDeleting) {
            shouldPrependColon = false;
            s.insert(s.length() - 1, ":");
        } else if (shouldAppendColon && !isDeleting) {
            shouldAppendColon = false;
            s.append(":");
        }
    }
}
