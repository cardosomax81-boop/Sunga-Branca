package br.com.sungabranca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val birthYear: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goal: String = "",
    val experienceLevel: String = "",
    val screeningCompletedAt: Long? = null,
    val screeningHasRiskFlag: Boolean = false
)