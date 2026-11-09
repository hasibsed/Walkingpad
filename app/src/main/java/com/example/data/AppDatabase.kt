package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.WorkoutDao
import com.example.data.model.IntervalProgramEntity
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutTelemetrySample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutSession::class,
        WorkoutTelemetrySample::class,
        IntervalProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "walkingpad_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default built-in interval routines
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).workoutDao()
                                seedDefaultPrograms(dao)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultPrograms(dao: WorkoutDao) {
            // Program 1: Fat Burn Walk
            dao.insertIntervalProgram(
                IntervalProgramEntity(
                    title = "Fat Burn Pace",
                    description = "Steady-state aerobic calorie burn with gentle warm-up and cool-down",
                    stagesJson = """[{"name":"Warm-up","speedKmh":2.5,"durationSeconds":180},{"name":"Brisk Stride","speedKmh":4.2,"durationSeconds":600},{"name":"Cool-down","speedKmh":2.5,"durationSeconds":180}]""",
                    isBuiltIn = true
                )
            )

            // Program 2: HIIT Speed Intervals
            dao.insertIntervalProgram(
                IntervalProgramEntity(
                    title = "HIIT Speed Intervals",
                    description = "Alternating power walking and brisk recovery bursts for maximum cardiovascular conditioning",
                    stagesJson = """[{"name":"Warm-up","speedKmh":3.0,"durationSeconds":120},{"name":"Sprint Interval 1","speedKmh":6.0,"durationSeconds":90},{"name":"Recovery Walk","speedKmh":3.5,"durationSeconds":90},{"name":"Sprint Interval 2","speedKmh":6.5,"durationSeconds":90},{"name":"Recovery Walk","speedKmh":3.5,"durationSeconds":90},{"name":"Cool-down","speedKmh":2.5,"durationSeconds":120}]""",
                    isBuiltIn = true
                )
            )

            // Program 3: Power Walk Progression
            dao.insertIntervalProgram(
                IntervalProgramEntity(
                    title = "Pyramid Progression",
                    description = "Gradually escalating speed ramps pushing pace from gentle walk to peak tempo",
                    stagesJson = """[{"name":"Level 1","speedKmh":2.5,"durationSeconds":120},{"name":"Level 2","speedKmh":3.5,"durationSeconds":180},{"name":"Level 3 Peak","speedKmh":5.0,"durationSeconds":240},{"name":"Level 4 Sprint","speedKmh":6.0,"durationSeconds":120},{"name":"Cool-down","speedKmh":3.0,"durationSeconds":180}]""",
                    isBuiltIn = true
                )
            )
        }
    }
}
