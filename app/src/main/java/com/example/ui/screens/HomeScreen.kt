package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PunchClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TerritoryViewModel
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String,
    userId: String,
    territoryViewModel: TerritoryViewModel,
    onNavigateToLiveRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Load active user's DB metrics
    LaunchedEffect(userId) {
        territoryViewModel.loadUserData(userId)
    }

    val activities by territoryViewModel.activities.collectAsState()
    val territories by territoryViewModel.territories.collectAsState()

    // Aggregate statistics
    val totalRuns = activities.size
    val totalDistance = activities.sumOf { it.distance }
    val totalTerritories = territories.size
    val totalArea = territories.sumOf { it.area } // in square meters

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp) // Leave safety gap for bottom tab navigators
        ) {
            // Header welcome section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RunPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Runner avatar",
                        tint = RunPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "WELCOME BACK",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = userName,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("welcome_user_tag")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val isSupabase = com.example.data.SupabaseService.isConfigured()
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSupabase) RunSecondary else GoldColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSupabase) "SUPABASE BACKEND ACTIVE" else "LOCAL OFFLINE ENGINE",
                            color = if (isSupabase) RunSecondary else GoldColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Empire Headline Card (Hero Statement)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "YOUR SOVEREIGN EMPIRE",
                        color = RunSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You currently control ${territories.size} sectors of the map.",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = if (totalRuns > 0) (totalTerritories.toFloat() / maxOf(totalRuns.toFloat(), 1f)).coerceIn(0f, 1f) else 0f,
                        color = RunSecondary,
                        trackColor = RunSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Text(
                text = "TOTAL STATISTICS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // Grid Layout stats card (Generous grid spaces)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dist Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("home_stat_distance"),
                    colors = CardDefaults.cardColors(containerColor = RunSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = RunPrimary, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f km", totalDistance),
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(text = "Total Distance", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Runs counts Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("home_stat_runs"),
                    colors = CardDefaults.cardColors(containerColor = RunSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(imageVector = Icons.Default.PunchClock, contentDescription = null, tint = GoldColor, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "$totalRuns",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(text = "Activities", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Full width territory area card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("home_stat_territory"),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RunSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = RunSecondary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (totalArea >= 10000) String.format(Locale.getDefault(), "%.1f k㎡", totalArea / 1_000_000.0)
                                       else String.format(Locale.getDefault(), "%.0f ㎡", totalArea),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(text = "Total Territory Area Controlled", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RunSecondary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$totalTerritories",
                            color = RunSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Primary Big start button
            Button(
                onClick = onNavigateToLiveRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("start_run_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RunPrimary),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, size = 26.dp, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "START NEW RUN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Icon ext helper to avoid size checks
@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
}
