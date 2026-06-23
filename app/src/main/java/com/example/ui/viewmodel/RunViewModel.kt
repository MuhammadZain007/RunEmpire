package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.example.data.model.ActivityEntity
import com.example.data.model.LatLngPoint
import com.example.data.model.TerritoryEntity
import com.example.data.repo.RunRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface RunningState {
    object Idle : RunningState
    object Active : RunningState
    object Paused : RunningState
    data class Completed(val distance: Double, val duration: Int, val calories: Double, val wasLoop: Boolean, val area: Double) : RunningState
}

class RunViewModel(
    private val context: Context,
    private val repository: RunRepository
) : ViewModel() {

    private val _runningState = MutableStateFlow<RunningState>(RunningState.Idle)
    val runningState: StateFlow<RunningState> = _runningState.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLngPoint>>(emptyList())
    val routePoints: StateFlow<List<LatLngPoint>> = _routePoints.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _isSimulatedRun = MutableStateFlow(false) // Defaults to real GPS tracking
    val isSimulatedRun: StateFlow<Boolean> = _isSimulatedRun.asStateFlow()

    // Real GPS client
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    // Coroutine Jobs
    private var timerJob: Job? = null
    private var simulationJob: Job? = null

    // For Territory Captures UI Dialog popup
    private val _showNewTerritoryCaptured = MutableStateFlow<Double?>(null) // Contains area if captured, null otherwise
    val showNewTerritoryCaptured: StateFlow<Double?> = _showNewTerritoryCaptured.asStateFlow()

    // Predefined simulated route loop near a scenic park (forms a valid loop)
    private val simulatedPathPreset = listOf(
        LatLngPoint(37.77490, -122.41940), // Start (Origin)
        LatLngPoint(37.77580, -122.41910),
        LatLngPoint(37.77660, -122.41880),
        LatLngPoint(37.77740, -122.41850),
        LatLngPoint(37.77780, -122.41650), // Turning right
        LatLngPoint(37.77820, -122.41450),
        LatLngPoint(37.77800, -122.41250),
        LatLngPoint(37.77700, -122.41280), // Turning down
        LatLngPoint(37.77600, -122.41310),
        LatLngPoint(37.77490, -122.41350),
        LatLngPoint(37.77450, -122.41550), // Turning left back to start
        LatLngPoint(37.77470, -122.41750),
        LatLngPoint(37.77488, -122.41935)  // Near End (Forms loop: dist to start ~ 4 meters!)
    )

    fun toggleSimulation(enabled: Boolean) {
        if (_runningState.value == RunningState.Idle) {
            _isSimulatedRun.value = enabled
        }
    }

    @SuppressLint("MissingPermission")
    fun startRun() {
        if (_runningState.value != RunningState.Idle) return

        _runningState.value = RunningState.Active
        _routePoints.value = emptyList()
        _durationSeconds.value = 0
        _distanceKm.value = 0.0
        _currentSpeedKmh.value = 0.0
        _showNewTerritoryCaptured.value = null

        startTimer()

        if (_isSimulatedRun.value) {
            startSimulation()
        } else {
            startGpsTracking()
        }
    }

    fun pauseRun() {
        if (_runningState.value == RunningState.Active) {
            _runningState.value = RunningState.Paused
            pauseTimer()
            _currentSpeedKmh.value = 0.0
        }
    }

    fun resumeRun() {
        if (_runningState.value == RunningState.Paused) {
            _runningState.value = RunningState.Active
            startTimer()
        }
    }

    fun finishRun() {
        val currentState = _runningState.value
        if (currentState != RunningState.Active && currentState != RunningState.Paused) return

        // Stop all jobs
        stopTimer()
        stopGpsTracking()
        simulationJob?.cancel()

        val points = _routePoints.value
        val userId = repository.getCurrentUserId() ?: "anonymous_mvp_user"
        val dist = _distanceKm.value
        val dur = _durationSeconds.value
        val cals = calculateCalories(dist, dur)

        val isLoop = repository.isClosedLoop(points)
        var capturedArea = 0.0

        viewModelScope.launch {
            if (isLoop && points.size >= 4) {
                capturedArea = repository.calculatePolygonArea(points)
                if (capturedArea > 0.0) {
                    val territory = TerritoryEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        polygon = points,
                        area = capturedArea,
                        capturedAt = System.currentTimeMillis()
                    )
                    repository.insertTerritory(territory)
                    _showNewTerritoryCaptured.value = capturedArea
                }
            }

            // Save Activity
            val activity = ActivityEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                distance = dist,
                duration = dur,
                calories = cals,
                route = points,
                createdAt = System.currentTimeMillis()
            )
            repository.insertActivity(activity)

            _runningState.value = RunningState.Completed(
                distance = dist,
                duration = dur,
                calories = cals,
                wasLoop = isLoop && capturedArea > 0.0,
                area = capturedArea
            )
        }
    }

    fun resetRun() {
        _runningState.value = RunningState.Idle
        _routePoints.value = emptyList()
        _durationSeconds.value = 0
        _distanceKm.value = 0.0
        _currentSpeedKmh.value = 0.0
        _showNewTerritoryCaptured.value = null
    }

    fun dismissTerritoryAlert() {
        _showNewTerritoryCaptured.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _durationSeconds.value += 1
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var stepIndex = 0
            while (_runningState.value == RunningState.Active || _runningState.value == RunningState.Paused) {
                if (_runningState.value == RunningState.Active) {
                    if (stepIndex < simulatedPathPreset.size) {
                        val nextPoint = simulatedPathPreset[stepIndex]
                        addLocationPoint(nextPoint.latitude, nextPoint.longitude)
                        _currentSpeedKmh.value = 11.2 + (-0.5 + Math.random() * 1.0) // 11 km/h variable jog speed
                        stepIndex += 1
                    } else {
                        // Keep jogging slightly at the end loop point to avoid out of bounds
                        val last = simulatedPathPreset.last()
                        addLocationPoint(last.latitude + (-0.00002 + Math.random() * 0.00004), last.longitude + (-0.00002 + Math.random() * 0.00004))
                    }
                }
                delay(3000) // Update simulation coordinates every 3 seconds
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsTracking() {
        stopGpsTracking()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateDistanceMeters(1.5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                if (_runningState.value != RunningState.Active) return
                val loc = result.lastLocation ?: return
                
                // Set speed to km/h
                val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
                _currentSpeedKmh.value = speedKmh
                
                addLocationPoint(loc.latitude, loc.longitude)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopGpsTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun addLocationPoint(lat: Double, lng: Double) {
        val currentList = _routePoints.value
        val newPoint = LatLngPoint(lat, lng)
        val updatedList = currentList + newPoint
        _routePoints.value = updatedList

        if (currentList.isNotEmpty()) {
            val prev = currentList.last()
            val segmentDistInMeters = repository.calculateHaversineDistance(
                prev.latitude, prev.longitude,
                newPoint.latitude, newPoint.longitude
            )
            // Add segment distance to cumulative counter
            _distanceKm.value += (segmentDistInMeters / 1000.0)
        }
    }

    private fun calculateCalories(distanceKm: Double, durationSeconds: Int): Double {
        // Base estimate: ~65 calories burned per km jogged, adjusted slightly for runner's time
        val calorieByDist = distanceKm * 65.0
        val calorieByTime = (durationSeconds / 60.0) * 8.5 // 8.5 kcal per minute
        return if (distanceKm > 0.1) calorieByDist else calorieByTime
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopGpsTracking()
        simulationJob?.cancel()
    }
}

class RunViewModelFactory(
    private val context: Context,
    private val repository: RunRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RunViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
