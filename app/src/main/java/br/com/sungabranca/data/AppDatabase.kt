package br.com.sungabranca.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutItemEntity::class,
        SessionEntity::class,
        SetLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}