package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BeltState
import com.example.bluetooth.ConnectionStatus
import com.example.bluetooth.DiscoveredBleDevice
import com.example.bluetooth.LiveTreadmillData
import com.example.bluetooth.WalkingPadBleConstants
import com.example.bluetooth.WalkingPadBleManager
import com.example.data.AppDatabase
import com.example.data.WorkoutRepository
import com.example.data.dao.AggregateStats
import com.example.data.model.IntervalProgramEntity
import com.example.data.model.IntervalStage
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutTelemetrySample
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class GoalType {
    NONE,
    DISTANCE,  // in km
    TIME,      // in minutes
    CALORIES   // in kcal
}

class WalkingPadViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = WalkingPadBleManager(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = WorkoutRepository(database.workoutDao(), application)

    // BLE state passthrough
    val connectionStatus: StateFlow<ConnectionStatus> = bleManager.connectionStatus
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = bleManager.discoveredDevices
    val connectedDeviceName: StateFlow<String?> = bleManager.connectedDeviceName
    val connectedDeviceAddress: StateFlow<String?> = bleManager.connectedDeviceAddress
    val beltState: StateFlow<BeltState> = bleManager.beltState
    val liveData: StateFlow<LiveTreadmillData> = bleManager.liveData
    val lastErrorMessage: StateFlow<String?> = bleManager.lastErrorMessage

    // Speed Control
    private val _targetSpeedKmh = MutableStateFlow(3.0f)
    val targetSpeedKmh: StateFlow<Float> = _targetSpeedKmh.asStateFlow()

    // Active Workout Tracking
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _sessionDurationSec = MutableStateFlow(0)
    val sessionDurationSec: StateFlow<Int> = _sessionDurationSec.asStateFlow()

    private val _sessionDistanceMeters = MutableStateFlow(0.0f)
    val sessionDistanceMeters: StateFlow<Float> = _sessionDistanceMeters.asStateFlow()

    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    private val _sessionCalories = MutableStateFlow(0.0f)
    val sessionCalories: StateFlow<Float> = _sessionCalories.asStateFlow()

    private val _workoutMode = MutableStateFlow("MANUAL")
    val workoutMode: StateFlow<String> = _workoutMode.asStateFlow()

    private val _goalType = MutableStateFlow(GoalType.NONE)
    val goalType: StateFlow<GoalType> = _goalType.asStateFlow()

    private val _goalTargetValue = MutableStateFlow(0.0f)
    val goalTargetValue: StateFlow<Float> = _goalTargetValue.asStateFlow()

    private val _goalProgress = MutableStateFlow(0.0f) // 0.0 to 1.0
    val goalProgress: StateFlow<Float> = _goalProgress.asStateFlow()

    // Interval State Machine
    private val _activeIntervalProgram = MutableStateFlow<IntervalProgramEntity?>(null)
    val activeIntervalProgram: StateFlow<IntervalProgramEntity?> = _activeIntervalProgram.asStateFlow()

    private val _currentIntervalStages = MutableStateFlow<List<IntervalStage>>(emptyList())
    val currentIntervalStages: StateFlow<List<IntervalStage>> = _currentIntervalStages.asStateFlow()

    private val _currentStageIndex = MutableStateFlow(0)
    val currentStageIndex: StateFlow<Int> = _currentStageIndex.asStateFlow()

    private val _stageRemainingSec = MutableStateFlow(0)
    val stageRemainingSec: StateFlow<Int> = _stageRemainingSec.asStateFlow()

    private val _isIntervalActive = MutableStateFlow(false)
    val isIntervalActive: StateFlow<Boolean> = _isIntervalActive.asStateFlow()

    // Repository Flows
    val allSessions: StateFlow<List<WorkoutSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aggregateStats: StateFlow<AggregateStats?> = repository.aggregateStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val intervalPrograms: StateFlow<List<IntervalProgramEntity>> = repository.intervalPrograms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Internal session telemetry recording buffer
    private val telemetryBuffer = mutableListOf<WorkoutTelemetrySample>()
    private var sessionStartTimeMs: Long = 0
    private var workoutTickerJob: Job? = null
    private var baselineSteps: Int = 0
    private var baselineDistance: Float = 0.0f
    private var maxSpeedReached: Float = 0.0f

    init {
        // Collect live data updates to sync baseline values when belt starts
        viewModelScope.launch {
            liveData.collect { data ->
                if (_isSessionActive.value) {
                    if (data.instantaneousSpeedKmh > maxSpeedReached) {
                        maxSpeedReached = data.instantaneousSpeedKmh
                    }
                }
            }
        }
    }

    // --- BLE Control Methods ---

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(address: String) {
        bleManager.connectToDevice(address)
    }

    fun disconnectDevice() {
        bleManager.disconnect()
    }

    fun startBelt() {
        bleManager.startOrResume()
        bleManager.setTargetSpeed(_targetSpeedKmh.value)
        if (!_isSessionActive.value) {
            startWorkoutSession(mode = "MANUAL", goal = GoalType.NONE, goalVal = 0f)
        }
    }

    fun pauseBelt() {
        bleManager.pause()
    }

    fun emergencyStop() {
        vibrate(300)
        bleManager.emergencyStop()
        if (_isSessionActive.value) {
            finishAndSaveWorkout()
        }
        stopIntervalProgram()
    }

    fun setTargetSpeed(speed: Float) {
        val clamped = speed.coerceIn(WalkingPadBleConstants.MIN_SPEED_KMH, WalkingPadBleConstants.MAX_SPEED_KMH)
        val rounded = Math.round(clamped * 10f) / 10f
        _targetSpeedKmh.value = rounded
        if (beltState.value == BeltState.RUNNING || beltState.value == BeltState.STARTING) {
            bleManager.setTargetSpeed(rounded)
        }
    }

    fun adjustSpeed(delta: Float) {
        setTargetSpeed(_targetSpeedKmh.value + delta)
    }

    // --- Workout Sessions & Goals ---

    fun startWorkoutSession(mode: String, goal: GoalType, goalVal: Float) {
        _isSessionActive.value = true
        _workoutMode.value = mode
        _goalType.value = goal
        _goalTargetValue.value = goalVal
        _goalProgress.value = 0.0f
        _sessionDurationSec.value = 0
        _sessionDistanceMeters.value = 0.0f
        _sessionSteps.value = 0
        _sessionCalories.value = 0.0f
        telemetryBuffer.clear()
        sessionStartTimeMs = System.currentTimeMillis()
        maxSpeedReached = _targetSpeedKmh.value
        baselineSteps = liveData.value.totalSteps
        baselineDistance = liveData.value.totalDistanceMeters

        startTicker()
    }

    private fun startTicker() {
        workoutTickerJob?.cancel()
        workoutTickerJob = viewModelScope.launch {
            while (_isSessionActive.value) {
                delay(1000)
                if (beltState.value == BeltState.RUNNING) {
                    _sessionDurationSec.value += 1
                    val currentLive = liveData.value

                    // Calculate delta distance or estimate from speed if sensor lacks incremental distance
                    val currentDist = if (currentLive.totalDistanceMeters >= baselineDistance) {
                        currentLive.totalDistanceMeters - baselineDistance
                    } else {
                        // Estimate distance in meters: speed (km/h) / 3.6 * 1 sec
                        _sessionDistanceMeters.value + (currentLive.instantaneousSpeedKmh / 3.6f)
                    }
                    _sessionDistanceMeters.value = currentDist

                    // Steps
                    val currentSteps = if (currentLive.totalSteps >= baselineSteps) {
                        currentLive.totalSteps - baselineSteps
                    } else {
                        _sessionSteps.value + (currentLive.stepCadenceSpm / 60)
                    }
                    _sessionSteps.value = currentSteps

                    // Calories: approximate 0.05 kcal per meter at typical brisk walk/jog
                    val calories = (currentDist * 0.045f).coerceAtLeast(currentLive.instantaneousKcal)
                    _sessionCalories.value = calories

                    // Record telemetry point
                    telemetryBuffer.add(
                        WorkoutTelemetrySample(
                            sessionId = 0,
                            timestampMs = System.currentTimeMillis(),
                            elapsedSeconds = _sessionDurationSec.value,
                            speedKmh = currentLive.instantaneousSpeedKmh,
                            cadenceSpm = currentLive.stepCadenceSpm,
                            cumulativeDistanceMeters = currentDist,
                            instantaneousCalories = calories
                        )
                    )

                    // Check Goals
                    updateGoalProgress()

                    // Check Interval Step Countdown
                    if (_isIntervalActive.value) {
                        tickInterval()
                    }
                }
            }
        }
    }

    private fun updateGoalProgress() {
        when (_goalType.value) {
            GoalType.DISTANCE -> {
                val targetMeters = _goalTargetValue.value * 1000f
                if (targetMeters > 0) {
                    val progress = (_sessionDistanceMeters.value / targetMeters).coerceIn(0.0f, 1.0f)
                    _goalProgress.value = progress
                    if (progress >= 1.0f) {
                        vibrate(500)
                    }
                }
            }
            GoalType.TIME -> {
                val targetSeconds = _goalTargetValue.value * 60f
                if (targetSeconds > 0) {
                    val progress = (_sessionDurationSec.value / targetSeconds).coerceIn(0.0f, 1.0f)
                    _goalProgress.value = progress
                    if (progress >= 1.0f) {
                        vibrate(500)
                    }
                }
            }
            GoalType.CALORIES -> {
                val targetKcal = _goalTargetValue.value
                if (targetKcal > 0) {
                    val progress = (_sessionCalories.value / targetKcal).coerceIn(0.0f, 1.0f)
                    _goalProgress.value = progress
                    if (progress >= 1.0f) {
                        vibrate(500)
                    }
                }
            }
            GoalType.NONE -> {
                _goalProgress.value = 0.0f
            }
        }
    }

    fun finishAndSaveWorkout() {
        if (!_isSessionActive.value) return
        val duration = _sessionDurationSec.value
        val distance = _sessionDistanceMeters.value
        val steps = _sessionSteps.value
        val calories = _sessionCalories.value.toInt()
        val avgSpeed = if (duration > 0) (distance / duration) * 3.6f else 0.0f

        val session = WorkoutSession(
            startTime = sessionStartTimeMs,
            endTime = System.currentTimeMillis(),
            durationSeconds = duration,
            totalDistanceMeters = distance,
            totalSteps = steps,
            caloriesBurned = calories,
            avgSpeedKmh = Math.round(avgSpeed * 10f) / 10f,
            maxSpeedKmh = Math.round(maxSpeedReached * 10f) / 10f,
            mode = _workoutMode.value,
            targetType = if (_goalType.value != GoalType.NONE) _goalType.value.name else null,
            targetValue = if (_goalType.value != GoalType.NONE) _goalTargetValue.value else null
        )

        val samples = telemetryBuffer.toList()

        viewModelScope.launch {
            if (duration >= 5) { // Only save workouts longer than 5 seconds
                repository.saveWorkoutSession(session, samples)
            }
        }

        _isSessionActive.value = false
        workoutTickerJob?.cancel()
    }

    // --- Interval Training Runner ---

    fun startIntervalProgram(program: IntervalProgramEntity) {
        val stages = parseStagesJson(program.stagesJson)
        if (stages.isEmpty()) return

        _activeIntervalProgram.value = program
        _currentIntervalStages.value = stages
        _currentStageIndex.value = 0
        _stageRemainingSec.value = stages[0].durationSeconds
        _isIntervalActive.value = true

        // Apply first stage speed
        setTargetSpeed(stages[0].speedKmh)
        startBelt()

        startWorkoutSession(mode = "INTERVAL", goal = GoalType.NONE, goalVal = 0f)
        vibrate(200)
    }

    private fun tickInterval() {
        val remaining = _stageRemainingSec.value - 1
        if (remaining <= 0) {
            val nextIndex = _currentStageIndex.value + 1
            val stages = _currentIntervalStages.value
            if (nextIndex < stages.size) {
                // Advance to next stage
                _currentStageIndex.value = nextIndex
                val nextStage = stages[nextIndex]
                _stageRemainingSec.value = nextStage.durationSeconds
                setTargetSpeed(nextStage.speedKmh)
                vibrateDouble()
            } else {
                // Program completed
                _isIntervalActive.value = false
                vibrate(600)
                // Gradually slow down to cool down
                setTargetSpeed(WalkingPadBleConstants.MIN_SPEED_KMH)
            }
        } else {
            _stageRemainingSec.value = remaining
            // Alert on last 3 seconds
            if (remaining in 1..3) {
                vibrate(80)
            }
        }
    }

    fun stopIntervalProgram() {
        _isIntervalActive.value = false
        _activeIntervalProgram.value = null
        _currentIntervalStages.value = emptyList()
    }

    fun saveCustomIntervalProgram(title: String, description: String, stages: List<IntervalStage>) {
        val jsonArray = JSONArray()
        for (stage in stages) {
            jsonArray.put(JSONObject().apply {
                put("name", stage.name)
                put("speedKmh", stage.speedKmh.toDouble())
                put("durationSeconds", stage.durationSeconds)
            })
        }
        val entity = IntervalProgramEntity(
            title = title,
            description = description,
            stagesJson = jsonArray.toString(),
            isBuiltIn = false
        )
        viewModelScope.launch {
            repository.saveIntervalProgram(entity)
        }
    }

    fun deleteIntervalProgram(id: Long) {
        viewModelScope.launch {
            repository.deleteIntervalProgram(id)
        }
    }

    private fun parseStagesJson(json: String): List<IntervalStage> {
        return try {
            val list = mutableListOf<IntervalStage>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    IntervalStage(
                        name = obj.getString("name"),
                        speedKmh = obj.getDouble("speedKmh").toFloat(),
                        durationSeconds = obj.getInt("durationSeconds")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Export & History ---

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun exportSessionCsv(sessionId: Long) {
        viewModelScope.launch {
            try {
                val file = repository.exportSessionToCsv(sessionId)
                repository.shareExportedFile(file)
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    fun exportSessionJson(sessionId: Long) {
        viewModelScope.launch {
            try {
                val file = repository.exportSessionToJson(sessionId)
                repository.shareExportedFile(file)
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    fun getTelemetryForSession(sessionId: Long) = repository.getTelemetryForSession(sessionId)

    // --- Haptics Utility ---

    private fun vibrate(millis: Long) {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (e: Exception) {
            // Ignored if device lacks vibrator
        }
    }

    private fun vibrateDouble() {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 150)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTickerJob?.cancel()
        bleManager.disconnect()
    }
}
