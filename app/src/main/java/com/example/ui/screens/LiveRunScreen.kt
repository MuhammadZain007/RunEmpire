package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.DigitalMapView
import com.example.ui.theme.*
import com.example.ui.viewmodel.RunningState
import com.example.ui.viewmodel.RunViewModel
import java.util.Locale

@Composable
fun LiveRunScreen(
    runViewModel: RunViewModel,
    onNavigateBack: () -> Unit,
    onRunFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe Run State from ViewModel
    val runningState by runViewModel.runningState.collectAsState()
    val routePoints by runViewModel.routePoints.collectAsState()
    val durationSeconds by runViewModel.durationSeconds.collectAsState()
    val distanceKm by runViewModel.distanceKm.collectAsState()
    val speedKmh by runViewModel.currentSpeedKmh.collectAsState()
    val isSimulated by runViewModel.isSimulatedRun.collectAsState()

    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
    }

    // Request permissions on screen launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Automatically navigate to summary when completed
    LaunchedEffect(runningState) {
        if (runningState is RunningState.Completed) {
            onRunFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
    ) {
        // Full screen map tracking layer
        DigitalMapView(
            points = routePoints,
            modifier = Modifier.fillMaxSize(),
            emptyText = if (isSimulated) "Virtual GPS Simulator is active..." else "Seeking fine GPS satellites..."
        )

        // Top Section: Live Metrics Display Overlay (Semi-transparent Slate Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = RunBackground.copy(alpha = 0.92f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, RunSurfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            runViewModel.resetRun()
                            onNavigateBack()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close run page", tint = TextPrimary)
                    }

                    // Simulation details
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSimulated) RunSecondary.copy(alpha = 0.15f) else RunPrimary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("simulated_mode_badge")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSimulated) RunSecondary else RunPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulated) "GPS SIMULATION ACTIVE" else "LIVE GPS OUTDOORS",
                                color = if (isSimulated) RunSecondary else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. Distance
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f", distanceKm),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("live_distance_text")
                        )
                        Text(
                            text = "DISTANCE (KM)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(48.dp).background(RunSurfaceVariant))

                    // 2. Duration
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatDuration(durationSeconds),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("live_duration_text")
                        )
                        Text(
                            text = "DURATION",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(48.dp).background(RunSurfaceVariant))

                    // 3. Current Speed
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", speedKmh),
                            color = RunSecondary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("live_speed_text")
                        )
                        Text(
                            text = "SPEED (KM/H)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Location Warn overlay banner if permission denied
        if (!hasLocationPermission && !isSimulated) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.GpsOff, contentDescription = "GPS Disabled Icon", tint = ErrorColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "GPS Permissions Restricted",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Run Empire needs GPS core permissions to log your routes! Choose Simulation mode, or enable permissions in system diagnostics.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { runViewModel.toggleSimulation(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = RunSecondary)
                        ) {
                            Text("Use Simulator", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Retry Permissions")
                        }
                    }
                }
            }
        }

        // Bottom section: Control Buttons & Sim Switch Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Interactive simulator configuration when in IDLE
            if (runningState == RunningState.Idle) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = RunSurface.copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = RunSecondary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Virtual GPS Simulator", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Simulates looping path around park", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isSimulated,
                            onCheckedChange = { runViewModel.toggleSimulation(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RunSecondary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = RunSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Big, bold fitness custom touch actions!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (runningState) {
                    RunningState.Idle -> {
                        Button(
                            onClick = { runViewModel.startRun() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("start_gps_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RunPrimary),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("START ACTIVITY", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }

                    RunningState.Active -> {
                        Button(
                            onClick = { runViewModel.pauseRun() },
                            modifier = Modifier
                                .size(80.dp)
                                .testTag("pause_gps_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldColor),
                            shape = CircleShape
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause run", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    RunningState.Paused -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Resume Button (Glow Green)
                            Button(
                                onClick = { runViewModel.resumeRun() },
                                modifier = Modifier
                                    .size(76.dp)
                                    .testTag("resume_gps_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RunSecondary),
                                shape = CircleShape
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume run", tint = Color.White, modifier = Modifier.size(32.dp))
                            }

                            // Finish Button (Glow stop Red)
                            Button(
                                onClick = { runViewModel.finishRun() },
                                modifier = Modifier
                                    .size(80.dp)
                                    .testTag("finish_gps_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                                shape = CircleShape
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop and complete run", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

// Convert seconds back to elegant screen timers (HH:MM:SS or MM:SS)
fun formatDuration(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
