package com.ymid.wakeonlan.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.persistence.entities.ActionLogEntity
import com.ymid.wakeonlan.persistence.entities.ActionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<ActionLogEntity, HistoryAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val marker: View = view.findViewById(R.id.history_icon)
        val deviceName: TextView = view.findViewById(R.id.history_device_name)
        val actionLabel: TextView = view.findViewById(R.id.history_action_label)
        val timestamp: TextView = view.findViewById(R.id.history_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.deviceName.text = item.deviceName
        holder.timestamp.text = dateFormat.format(Date(item.timestamp)).lowercase(Locale.getDefault())

        val ctx = holder.itemView.context
        val wake = item.actionType == ActionType.WAKE
        val label = ctx.getString(if (wake) R.string.history_action_wake else R.string.history_action_shutdown)
        holder.actionLabel.text = "» " + label.lowercase(Locale.getDefault())
        holder.actionLabel.setTextColor(ContextCompat.getColor(ctx, if (wake) R.color.ledOnlineColor else R.color.ledOfflineColor))
        holder.marker.setBackgroundResource(if (wake) R.drawable.device_status_online else R.drawable.device_status_offline)
        holder.marker.contentDescription = label
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ActionLogEntity>() {
            override fun areItemsTheSame(a: ActionLogEntity, b: ActionLogEntity) = a.id == b.id
            override fun areContentsTheSame(a: ActionLogEntity, b: ActionLogEntity) = a == b
        }
    }
}
