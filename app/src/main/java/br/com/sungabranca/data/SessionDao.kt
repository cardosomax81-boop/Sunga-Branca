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

    @Delete
    suspend fun deleteSessions(sessions: List<SessionEntity>)

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

    @Query("SELECT DISTINCT exerciseId FROM set_logs")
    fun observeLoggedExerciseIds(): Flow<List<Long>>

    @Query(
        """
        SELECT s.id AS sessionId, s.startedAt AS startedAt,
               l.weightKg AS weightKg, l.reps AS reps
        FROM set_logs l
        INNER JOIN sessions s ON s.id = l.sessionId
        WHERE l.exerciseId = :exerciseId
        ORDER BY s.startedAt DESC, l.setNumber
        """
    )
    fun observeSetPoints(exerciseId: Long): Flow<List<SetPoint>>
}

data class SetPoint(
    val sessionId: Long,
    val startedAt: Long,
    val weightKg: Double,
    val reps: Int
)