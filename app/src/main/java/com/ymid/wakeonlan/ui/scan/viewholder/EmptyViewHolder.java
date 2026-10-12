package com.ymid.wakeonlan.ui.scan.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ymid.wakeonlan.R;

public class EmptyViewHolder extends RecyclerView.ViewHolder {

    private final TextView title;
    private final TextView hint;

    public EmptyViewHolder(View view) {
        super(view);
        title = view.findViewById(R.id.network_scan_empty_title);
        hint = view.findViewById(R.id.network_scan_empty_hint);
    }

    /** While a scan runs an empty list only means nothing has answered yet. */
    public void bind(boolean scanning) {
        title.setText(scanning ? R.string.scan_searching : R.string.network_scan_no_devices_found);
        hint.setVisibility(scanning ? View.GONE : View.VISIBLE);
    }
}
