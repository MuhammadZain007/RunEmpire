package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TerritoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityHistoryScreen(
    userId: String,
    territoryViewModel: TerritoryViewModel,
    onSelectActivity: (activityId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(userId) {
        territoryViewModel.loadUserData(userId)
    }

    val activities by territoryViewModel.activities.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .padding(16.dp)
            .padding(bottom = 72.dp) // Leave navigation margin
    ) {
        // Title Screen
        Text(
            text = "ATHLETE CHRONICLES",
            color = RunPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Activity Log",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RunSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Runs Logged Yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Step up onto the field! Your tracking chronicles will be safely displayed here.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activities) { activity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectActivity(activity.id) }
                            .testTag("activity_card_${activity.id}"),
                        colors = CardDefaults.cardColors(containerColor = RunSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Left icon: active loop banner or standard runner icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (activity.route.size >= 4) RunSecondary.copy(alpha = 0.15f)
                                            else RunPrimary.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (activity.route.size >= 4) Icons.Default.Map else Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        tint = if (activity.route.size >= 4) RunSecondary else RunPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(activity.createdAt)),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatDuration(activity.duration),
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.size(4.dp).background(TextSecondary, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f kcal", activity.calories),
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f km", activity.distance),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
