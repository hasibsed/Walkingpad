package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.dao.AggregateStats
import com.example.data.dao.WorkoutDao
import com.example.data.model.IntervalProgramEntity
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutTelemetrySample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutRepository(private val workoutDao: WorkoutDao, private val context: Context) {

    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()
    val aggregateStats: Flow<AggregateStats> = workoutDao.getAggregateStats()
    val intervalPrograms: Flow<List<IntervalProgramEntity>> = workoutDao.getAllIntervalPrograms()

    fun getTelemetryForSession(sessionId: Long): Flow<List<WorkoutTelemetrySample>> {
        return workoutDao.getTelemetryForSession(sessionId)
    }

    suspend fun saveWorkoutSession(
        session: WorkoutSession,
        samples: List<WorkoutTelemetrySample>
    ): Long = withContext(Dispatchers.IO) {
        val sessionId = workoutDao.insertSession(session)
        if (samples.isNotEmpty()) {
            val mappedSamples = samples.map { it.copy(sessionId = sessionId) }
            workoutDao.insertTelemetrySamples(mappedSamples)
        }
        sessionId
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteSessionById(sessionId)
    }

    suspend fun saveIntervalProgram(program: IntervalProgramEntity) = withContext(Dispatchers.IO) {
        workoutDao.insertIntervalProgram(program)
    }

    suspend fun deleteIntervalProgram(programId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteIntervalProgramById(programId)
    }

    suspend fun exportSessionToCsv(sessionId: Long): File = withContext(Dispatchers.IO) {
        val session = workoutDao.getSessionById(sessionId) ?: throw IllegalArgumentException("Session not found")
        val samples = workoutDao.getTelemetryListForSession(sessionId)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val fileName = "WalkingPad_Workout_${dateFormat.format(Date(session.startTime))}.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        file.printWriter().use { out ->
            out.println("# WalkingPad MC21 Workout Log")
            out.println("# Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(session.startTime))}")
            out.println("# Duration (sec),${session.durationSeconds}")
            out.println("# Total Distance (m),${session.totalDistanceMeters}")
            out.println("# Total Steps,${session.totalSteps}")
            out.println("# Calories (kcal),${session.caloriesBurned}")
            out.println("# Avg Speed (km/h),${session.avgSpeedKmh}")
            out.println("# Max Speed (km/h),${session.maxSpeedKmh}")
            out.println("# Mode,${session.mode}")
            out.println()
            out.println("ElapsedSeconds,SpeedKmh,CadenceSpm,DistanceMeters,InstantaneousKcal")
            for (s in samples) {
                out.println("${s.elapsedSeconds},${s.speedKmh},${s.cadenceSpm},${s.cumulativeDistanceMeters},${s.instantaneousCalories}")
            }
        }
        file
    }

    suspend fun exportSessionToJson(sessionId: Long): File = withContext(Dispatchers.IO) {
        val session = workoutDao.getSessionById(sessionId) ?: throw IllegalArgumentException("Session not found")
        val samples = workoutDao.getTelemetryListForSession(sessionId)

        val root = JSONObject().apply {
            put("app", "WalkingPad MC21 Controller")
            put("sessionId", session.id)
            put("startTime", session.startTime)
            put("endTime", session.endTime)
            put("durationSeconds", session.durationSeconds)
            put("totalDistanceMeters", session.totalDistanceMeters.toDouble())
            put("totalSteps", session.totalSteps)
            put("caloriesBurned", session.caloriesBurned)
            put("avgSpeedKmh", session.avgSpeedKmh.toDouble())
            put("maxSpeedKmh", session.maxSpeedKmh.toDouble())
            put("mode", session.mode)

            val samplesArray = JSONArray()
            for (s in samples) {
                samplesArray.put(JSONObject().apply {
                    put("elapsedSec", s.elapsedSeconds)
                    put("speedKmh", s.speedKmh.toDouble())
                    put("cadenceSpm", s.cadenceSpm)
                    put("distanceMeters", s.cumulativeDistanceMeters.toDouble())
                    put("kcal", s.instantaneousCalories.toDouble())
                })
            }
            put("telemetry", samplesArray)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val fileName = "WalkingPad_Workout_${dateFormat.format(Date(session.startTime))}.json"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(root.toString(2))
        file
    }

    fun shareExportedFile(file: File) {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            // Fallback to Uri.fromFile if fileprovider not yet configured
            android.net.Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".csv")) "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WalkingPad MC21 Workout Data")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Workout Log").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
