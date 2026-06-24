package com.example.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.LatLngPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.*

class RunningTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "RunningTrackingService"
        private const val NOTIFICATION_CHANNEL_ID = "running_tracking_channel"
        private const val NOTIFICATION_ID = 8899

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        // Static Flows that ViewModel and Views can observe in real time
        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused = _isPaused.asStateFlow()

        private val _routePoints = MutableStateFlow<List<LatLngPoint>>(emptyList())
        val routePoints = _routePoints.asStateFlow()

        private val _distanceKm = MutableStateFlow(0.0)
        val distanceKm = _distanceKm.asStateFlow()

        private val _durationSeconds = MutableStateFlow(0)
        val durationSeconds = _durationSeconds.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0.0)
        val currentSpeedKmh = _currentSpeedKmh.asStateFlow()

        fun resetServiceState() {
            _isTracking.value = false
            _isPaused.value = false
            _routePoints.value = emptyList()
            _distanceKm.value = 0.0
            _durationSeconds.value = 0
            _currentSpeedKmh.value = 0.0
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d(TAG, "Service onStartCommand with action: $action")
            when (action) {
                ACTION_START -> {
                    startTrackingService()
                }
                ACTION_PAUSE -> {
                    pauseTrackingService()
                }
                ACTION_RESUME -> {
                    resumeTrackingService()
                }
                ACTION_STOP -> {
                    stopTrackingService()
                }
            }
        }
        return START_STICKY
    }

    private fun startTrackingService() {
        resetServiceState()
        _isTracking.value = true
        _isPaused.value = false

        startForeground(NOTIFICATION_ID, buildNotification())
        startTimer()
        startGpsTracking()
    }

    private fun pauseTrackingService() {
        _isPaused.value = true
        _currentSpeedKmh.value = 0.0
        timerJob?.cancel()
        stopGpsTracking()
        updateNotification()
    }

    private fun resumeTrackingService() {
        _isPaused.value = false
        startTimer()
        startGpsTracking()
        updateNotification()
    }

    private fun stopTrackingService() {
        _isTracking.value = false
        _isPaused.value = false
        timerJob?.cancel()
        stopGpsTracking()
        stopForeground(true)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                _durationSeconds.value += 1
                if (_durationSeconds.value % 5 == 0) {
                    updateNotification()
                }
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
                if (!_isTracking.value || _isPaused.value) return
                val loc = result.lastLocation ?: return

                val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
                _currentSpeedKmh.value = speedKmh

                addLocationPoint(loc.latitude, loc.longitude)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS updates", e)
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
            val segmentDistInMeters = calculateHaversineDistance(
                prev.latitude, prev.longitude,
                newPoint.latitude, newPoint.longitude
            )
            _distanceKm.value += (segmentDistInMeters / 1000.0)
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Run Empire GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and status of your active jog/run."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationStr = formatDuration(_durationSeconds.value)
        val distanceStr = String.format(Locale.getDefault(), "%.2f km", _distanceKm.value)
        val contentText = "Distance: $distanceStr | Duration: $durationStr"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Active Jog in Progress")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
