package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.IntervalProgramEntity
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutTelemetrySample
import kotlinx.coroutines.flow.Flow

data class AggregateStats(
    val totalWorkouts: Int,
    val totalDistanceMeters: Float,
    val totalDurationSeconds: Long,
    val totalSteps: Long,
    val totalCalories: Long,
    val maxSpeedKmh: Float
)

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllSessions()

    // Telemetry Samples
    @Query("SELECT * FROM workout_telemetry_samples WHERE sessionId = :sessionId ORDER BY elapsedSeconds ASC")
    fun getTelemetryForSession(sessionId: Long): Flow<List<WorkoutTelemetrySample>>

    @Query("SELECT * FROM workout_telemetry_samples WHERE sessionId = :sessionId ORDER BY elapsedSeconds ASC")
    suspend fun getTelemetryListForSession(sessionId: Long): List<WorkoutTelemetrySample>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetrySamples(samples: List<WorkoutTelemetrySample>)

    // Aggregates
    @Query("""
        SELECT 
            COUNT(*) as totalWorkouts,
            COALESCE(SUM(totalDistanceMeters), 0.0) as totalDistanceMeters,
            COALESCE(SUM(durationSeconds), 0) as totalDurationSeconds,
            COALESCE(SUM(totalSteps), 0) as totalSteps,
            COALESCE(SUM(caloriesBurned), 0) as totalCalories,
            COALESCE(MAX(maxSpeedKmh), 0.0) as maxSpeedKmh
        FROM workout_sessions
    """)
    fun getAggregateStats(): Flow<AggregateStats>

    // Interval Programs
    @Query("SELECT * FROM interval_programs ORDER BY id ASC")
    fun getAllIntervalPrograms(): Flow<List<IntervalProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervalProgram(program: IntervalProgramEntity): Long

    @Query("DELETE FROM interval_programs WHERE id = :programId")
    suspend fun deleteIntervalProgramById(programId: Long)
}
