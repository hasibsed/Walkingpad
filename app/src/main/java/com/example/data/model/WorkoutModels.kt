package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val totalDistanceMeters: Float,
    val totalSteps: Int,
    val caloriesBurned: Int,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val mode: String, // MANUAL, INTERVAL, TARGET_GOAL
    val targetType: String? = null, // DISTANCE, TIME, CALORIES
    val targetValue: Float? = null,
    val notes: String? = null
)

@Entity(
    tableName = "workout_telemetry_samples",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class WorkoutTelemetrySample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestampMs: Long,
    val elapsedSeconds: Int,
    val speedKmh: Float,
    val cadenceSpm: Int,
    val cumulativeDistanceMeters: Float,
    val instantaneousCalories: Float
)

@Entity(tableName = "interval_programs")
data class IntervalProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val stagesJson: String,
    val isBuiltIn: Boolean = false
)

data class IntervalStage(
    val name: String,
    val speedKmh: Float,
    val durationSeconds: Int
)
