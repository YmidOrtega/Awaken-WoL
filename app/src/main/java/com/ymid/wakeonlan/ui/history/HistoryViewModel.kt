package com.ymid.wakeonlan.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ymid.wakeonlan.persistence.repository.ActionLogRepository

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    val logs = ActionLogRepository.getInstance(app).allLogs
}
