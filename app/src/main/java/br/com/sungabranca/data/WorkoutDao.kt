package br.com.sungabranca.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class WorkoutItemWithExercise(
    @Embedded val item: WorkoutItemEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts ORDER BY name")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Transaction
    @Query("SELECT * FROM workout_items WHERE workoutId = :workoutId ORDER BY position")
    fun observeItems(workoutId: Long): Flow<List<WorkoutItemWithExercise>>

    @Insert
    suspend fun insertItem(item: WorkoutItemEntity): Long

    @Update
    suspend fun updateItem(item: WorkoutItemEntity)

    @Delete
    suspend fun deleteItem(item: WorkoutItemEntity)
}