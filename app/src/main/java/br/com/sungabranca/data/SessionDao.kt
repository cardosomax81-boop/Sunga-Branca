package br.com.sungabranca.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY completedAt")
    fun observeSetLogs(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentSetLogs(exerciseId: Long, limit: Int): List<SetLogEntity>

    @Insert
    suspend fun insertSetLog(setLog: SetLogEntity): Long

    @Update
    suspend fun updateSetLog(setLog: SetLogEntity)

    @Delete
    suspend fun deleteSetLog(setLog: SetLogEntity)
}