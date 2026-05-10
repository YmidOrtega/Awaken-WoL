package com.ymid.wakeonlan.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ymid.wakeonlan.R;

public class TrustedWifiDialogFragment extends BottomSheetDialogFragment {

    public interface Callback {
        void onTrustedWifiSelection(Set<String> selected);
    }

    private static final String ARG_ENTRIES = "entries";
    private static final String ARG_VALUES = "values";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_MODE = "mode";

    public static final int MODE_SCAN = 0;  // Show all scanned networks with checkboxes
    public static final int MODE_VIEW = 1;  // Show only saved networks with delete buttons

    private Callback callback;

    public static TrustedWifiDialogFragment newInstance(String[] entries, String[] values, ArrayList<String> selected, int mode) {
        TrustedWifiDialogFragment fragment = new TrustedWifiDialogFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_ENTRIES, entries);
        args.putStringArray(ARG_VALUES, values);
        args.putStringArrayList(ARG_SELECTED, selected);
        args.putInt(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof Callback) {
            callback = (Callback) getParentFragment();
        } else if (context instanceof Callback) {
            callback = (Callback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_trusted_wifi, container, false);

        TextView titleView = view.findViewById(R.id.trusted_wifi_title);
        TextView emptyView = view.findViewById(R.id.trusted_wifi_empty);
        RecyclerView recyclerView = view.findViewById(R.id.trusted_wifi_list);
        MaterialButton doneButton = view.findViewById(R.id.trusted_wifi_done);

        ArrayList<String> selected = getArguments() != null ? getArguments().getStringArrayList(ARG_SELECTED) : new ArrayList<>();
        int mode = getArguments() != null ? getArguments().getInt(ARG_MODE, MODE_VIEW) : MODE_VIEW;

        // Set title based on mode
        if (mode == MODE_SCAN) {
            titleView.setText(R.string.pref_scan_wifi_title);
        } else {
            titleView.setText(R.string.pref_trusted_ssids_title);
        }

        List<WifiItem> items = buildItems(selected, mode);
        TrustedWifiAdapter adapter = new TrustedWifiAdapter(items, mode);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        boolean hasItems = !items.isEmpty();
        if (hasItems) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        doneButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onTrustedWifiSelection(adapter.getSelectedValues());
            }
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }
    }

    private List<WifiItem> buildItems(ArrayList<String> selected, int mode) {
        List<WifiItem> items = new ArrayList<>();

        String[] entries = getArguments() != null ? getArguments().getStringArray(ARG_ENTRIES) : null;
        String[] values = getArguments() != null ? getArguments().getStringArray(ARG_VALUES) : null;
        Set<String> selectedSet = selected != null ? new HashSet<>(selected) : new HashSet<>();

        if (mode == MODE_SCAN) {
            // Show all scanned networks with checkboxes
            if (entries != null && values != null) {
                for (int i = 0; i < Math.min(entries.length, values.length); i++) {
                    if (values[i] != null && !values[i].trim().isEmpty()) {
                        WifiItem item = new WifiItem(entries[i], values[i]);
                        item.isChecked = selectedSet.contains(values[i]);
                        items.add(item);
                    }
                }
            }
        } else {
            // MODE_VIEW: Show only saved networks
            if (selected != null) {
                for (String value : selected) {
                    if (value != null && !value.trim().isEmpty()) {
                        items.add(new WifiItem(value, value));
                    }
                }
            }
        }

        return items;
    }

    private static class WifiItem {
        private final String displayName;
        private final String value;
        private boolean isChecked;

        WifiItem(String displayName, String value) {
            this.displayName = displayName;
            this.value = value;
            this.isChecked = false;
        }
    }

    private static class TrustedWifiAdapter extends RecyclerView.Adapter<TrustedWifiViewHolder> {

        private final List<WifiItem> items;
        private final int mode;

        TrustedWifiAdapter(List<WifiItem> items, int mode) {
            this.items = items;
            this.mode = mode;
        }

        @NonNull
        @Override
        public TrustedWifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trusted_wifi, parent, false);
            return new TrustedWifiViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrustedWifiViewHolder holder, int position) {
            WifiItem item = items.get(position);
            if (mode == MODE_SCAN) {
                holder.bindCheckboxMode(item, isChecked -> {
                    item.isChecked = isChecked;
                });
            } else {
                holder.bindDeleteMode(item, () -> {
                    int currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        items.remove(currentPos);
                        notifyItemRemoved(currentPos);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        Set<String> getSelectedValues() {
            Set<String> selected = new HashSet<>();
            if (mode == MODE_SCAN) {
                for (WifiItem item : items) {
                    if (item.isChecked) {
                        selected.add(item.value);
                    }
                }
            } else {
                for (WifiItem item : items) {
                    selected.add(item.value);
                }
            }
            return selected;
        }
    }

    private static class TrustedWifiViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final com.google.android.material.checkbox.MaterialCheckBox checkBox;
        private final View deleteButton;

        TrustedWifiViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.trusted_wifi_name);
            checkBox = itemView.findViewById(R.id.trusted_wifi_checkbox);
            deleteButton = itemView.findViewById(R.id.trusted_wifi_delete);
        }

        void bindCheckboxMode(WifiItem item, OnCheckChangedListener listener) {
            nameView.setText(item.displayName);
            checkBox.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.GONE);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isChecked);

            View.OnClickListener clickListener = v -> {
                boolean newState = !checkBox.isChecked();
                checkBox.setChecked(newState);
                listener.onCheckChanged(newState);
            };

            itemView.setOnClickListener(clickListener);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onCheckChanged(isChecked);
            });
        }

        void bindDeleteMode(WifiItem item, Runnable onDelete) {
            nameView.setText(item.displayName);
            checkBox.setVisibility(View.GONE);
            deleteButton.setVisibility(View.VISIBLE);

            itemView.setOnClickListener(null);
            deleteButton.setOnClickListener(v -> onDelete.run());
        }

        interface OnCheckChangedListener {
            void onCheckChanged(boolean isChecked);
        }
    }
}
