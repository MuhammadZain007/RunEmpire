package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DigitalMapView
import com.example.ui.theme.*
import com.example.ui.viewmodel.TerritoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TerritoryMapScreen(
    userId: String,
    territoryViewModel: TerritoryViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(userId) {
        territoryViewModel.loadUserData(userId)
    }

    val territories by territoryViewModel.territories.collectAsState()

    // Extract list of Polygons
    val polygons = remember(territories) {
        territories.map { it.polygon }
    }

    val totalArea = remember(territories) {
        territories.sumOf { it.area }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .padding(16.dp)
            .padding(bottom = 72.dp) // Bottom padding safe space for tab bars
    ) {
        // Title Screen
        Text(
            text = "SOVEREIGN REALM",
            color = RunSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Your Captured Territories",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Huge interactive combined map of all captured territories
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .testTag("territories_map_contour"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RunSurface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DigitalMapView(
                    points = emptyList(), // No active run route
                    territoryPolygons = polygons,
                    modifier = Modifier.fillMaxSize(),
                    emptyText = "Claim your first territory by running a loop!"
                )

                // Overlay legends
                Card(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(RunSecondary, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sovereign Sectors (${territories.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom section: List of sectors
        Text(
            text = "SECTOR BLUEPRINTS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (territories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(RunSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.Landscape, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Territory Captured Yet", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Complete a continuous loop run (starting & ending within 50 meters) to annex sectors.", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(territories) { sector ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sector_item_${sector.id}"),
                        colors = CardDefaults.cardColors(containerColor = RunSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ANNEXED SECTOR #${sector.id.substring(0, 5).uppercase(Locale.getDefault())}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Captured ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(sector.capturedAt))}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (sector.area >= 10000) String.format(Locale.getDefault(), "%.1f k㎡", sector.area / 1_000_000.0)
                                           else String.format(Locale.getDefault(), "%.0f ㎡", sector.area),
                                    color = RunSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Polygon Area",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
