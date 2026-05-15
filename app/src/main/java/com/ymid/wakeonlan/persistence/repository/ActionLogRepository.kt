package com.ymid.wakeonlan.persistence.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.ymid.wakeonlan.persistence.DatabaseInstanceManager
import com.ymid.wakeonlan.persistence.entities.ActionLogEntity
import java.util.concurrent.Executors

class ActionLogRepository private constructor(context: Context) {

    private val dao = DatabaseInstanceManager.getInstance(context).actionLogDao()
    private val executor = Executors.newSingleThreadExecutor()

    val allLogs: LiveData<List<ActionLogEntity>> = dao.getAllAsLiveData()

    fun log(deviceName: String, actionType: String) {
        executor.execute {
            dao.insert(ActionLogEntity(deviceName = deviceName, actionType = actionType))
            dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        }
    }

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

        @Volatile private var INSTANCE: ActionLogRepository? = null

        fun getInstance(context: Context): ActionLogRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActionLogRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
