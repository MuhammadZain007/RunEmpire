package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PunchClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DigitalMapView
import com.example.ui.theme.*
import com.example.data.model.ActivityEntity
import com.example.data.repo.RunRepository
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    repository: RunRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activity by remember { mutableStateOf<ActivityEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(activityId) {
        repository.getActivityById(activityId).collectLatest {
            activity = it
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Session Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Navigate back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RunBackground)
            )
        },
        containerColor = RunBackground
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RunPrimary)
            }
        } else if (activity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Activity not found.", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            val act = activity!!
            val isLoop = repository.isClosedLoop(act.route)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Date Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = RunSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date(act.createdAt)),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Logged at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(act.createdAt))}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Map View Preview card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RunSurface)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DigitalMapView(
                            points = act.route,
                            territoryPolygons = if (isLoop) listOf(act.route) else emptyList(),
                            modifier = Modifier.fillMaxSize(),
                            emptyText = "Coordinates grid mapping..."
                        )
                    }
                }

                // If loop was completed, displays territory captured banner
                if (isLoop && act.route.size >= 4) {
                    val loopArea = repository.calculatePolygonArea(act.route)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = RunSecondary.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RunSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Celebration, contentDescription = null, tint = RunSecondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Captured Sovereign Sector", color = RunSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Loop Area: ${String.format(Locale.getDefault(), "%.0f ㎡", loopArea)} captured successfully.", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Stats values blocks
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = RunSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "ACTIVITY PERFORMANCE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. Distance
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = RunPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Distance", color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f km", act.distance),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = RunSurfaceVariant, thickness = 1.dp)

                        // 2. Duration
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PunchClock, contentDescription = null, tint = GoldColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Duration", color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = formatDuration(act.duration),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = RunSurfaceVariant, thickness = 1.dp)

                        // 3. Calories
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Est. Calories", color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f kcal", act.calories),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
