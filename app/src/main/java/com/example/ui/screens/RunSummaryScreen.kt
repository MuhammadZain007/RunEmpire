package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PunchClock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DigitalMapView
import com.example.ui.theme.*
import com.example.ui.viewmodel.RunningState
import com.example.ui.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RunSummaryScreen(
    runViewModel: RunViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val runningState by runViewModel.runningState.collectAsState()
    val routePoints by runViewModel.routePoints.collectAsState()
    val territoryCapturedArea by runViewModel.showNewTerritoryCaptured.collectAsState() // Double area if captured, else null

    // Cast the state safely to extract final statistics
    val summaryStats = remember(runningState) {
        if (runningState is RunningState.Completed) {
            runningState as RunningState.Completed
        } else {
            RunningState.Completed(0.0, 0, 0.0, false, 0.0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RUN COMPLETED",
                color = RunPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "Empire Expansion Logged",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic Territory Capture Celebration Card (Pristine Trophy style)
            if (summaryStats.wasLoop || territoryCapturedArea != null) {
                val area = territoryCapturedArea ?: summaryStats.area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("territory_captured_card"),
                    colors = CardDefaults.cardColors(containerColor = RunSecondary.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RunSecondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Trophy Icon",
                            tint = RunSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NEW TERRITORY CAPTURED!",
                            color = RunSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A complete closed running loop was detected. You have annexed this coordinates block into your offline territory map.",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Divider(color = RunSecondary.copy(alpha = 0.3f), thickness = 1.dp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("POLYGON SIZE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (area >= 10000) String.format(Locale.getDefault(), "%.2f k㎡", area / 1_000_000.0)
                                           else String.format(Locale.getDefault(), "%.0f ㎡", area),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ANNEX DATE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Friendly guide explaining loops
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = RunSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Loops Detected", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Note: Run starting and ending coordinates must finish within 50 meters of one another to capture territory.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Route Map Preview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RunSurface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DigitalMapView(
                        points = routePoints,
                        territoryPolygons = if (summaryStats.wasLoop) listOf(routePoints) else emptyList(),
                        modifier = Modifier.fillMaxSize(),
                        emptyText = "Reviewing completed path..."
                    )
                }
            }

            // Stats values blocks
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SESSION METRICS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Stats lines
                    // 1. Distance
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RunPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Distance Traveled", color = TextPrimary, fontSize = 14.sp)
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f km", summaryStats.distance),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("summary_stat_distance")
                        )
                    }
                    Divider(color = RunSurfaceVariant, thickness = 1.dp)

                    // 2. Duration
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PunchClock, contentDescription = null, tint = GoldColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Total Duration", color = TextPrimary, fontSize = 14.sp)
                        }
                        Text(
                            text = formatDuration(summaryStats.duration),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("summary_stat_duration")
                        )
                    }
                    Divider(color = RunSurfaceVariant, thickness = 1.dp)

                    // 3. Calories
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Est. Energy Burned", color = TextPrimary, fontSize = 14.sp)
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f kcal", summaryStats.calories),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("summary_stat_calories")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Complete save activity confirmation triggers back home
            Button(
                onClick = {
                    runViewModel.resetRun()
                    onNavigateHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_activity_button")
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RunPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SAVE & GO BACK HOME",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
