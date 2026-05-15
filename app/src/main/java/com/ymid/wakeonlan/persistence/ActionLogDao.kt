package com.ymid.wakeonlan.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ymid.wakeonlan.persistence.entities.ActionLogEntity

@Dao
interface ActionLogDao {

    @Query("SELECT * FROM ActionLog ORDER BY timestamp DESC LIMIT 300")
    fun getAllAsLiveData(): LiveData<List<ActionLogEntity>>

    @Insert
    fun insert(log: ActionLogEntity)

    @Query("DELETE FROM ActionLog WHERE timestamp < :cutoff")
    fun deleteOlderThan(cutoff: Long)
}
