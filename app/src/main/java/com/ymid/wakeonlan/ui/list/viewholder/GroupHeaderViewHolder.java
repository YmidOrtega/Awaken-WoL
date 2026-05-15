package com.ymid.wakeonlan.ui.list.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ymid.wakeonlan.R;

public class GroupHeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView label;

    public GroupHeaderViewHolder(View view) {
        super(view);
        label = view.findViewById(R.id.group_header_label);
    }

    public void bind(String groupName) {
        label.setText(groupName);
    }
}
