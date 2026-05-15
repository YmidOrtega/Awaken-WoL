package com.ymid.wakeonlan.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        val icon: ImageView = view.findViewById(R.id.history_icon)
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
        holder.timestamp.text = dateFormat.format(Date(item.timestamp))

        val ctx = holder.itemView.context
        if (item.actionType == ActionType.WAKE) {
            holder.icon.setImageResource(R.drawable.ic_power_24)
            holder.icon.contentDescription = ctx.getString(R.string.history_action_wake)
            holder.actionLabel.text = ctx.getString(R.string.history_action_wake)
        } else {
            holder.icon.setImageResource(R.drawable.ic_power_off_24)
            holder.icon.contentDescription = ctx.getString(R.string.history_action_shutdown)
            holder.actionLabel.text = ctx.getString(R.string.history_action_shutdown)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ActionLogEntity>() {
            override fun areItemsTheSame(a: ActionLogEntity, b: ActionLogEntity) = a.id == b.id
            override fun areContentsTheSame(a: ActionLogEntity, b: ActionLogEntity) = a == b
        }
    }
}
