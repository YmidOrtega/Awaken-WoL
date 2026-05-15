package com.ymid.wakeonlan.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ActionLog")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "device_name")
    val deviceName: String,

    @ColumnInfo(name = "action_type")
    val actionType: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

object ActionType {
    const val WAKE = "wake"
    const val SHUTDOWN = "shutdown"
}
