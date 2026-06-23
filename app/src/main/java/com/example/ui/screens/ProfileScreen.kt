package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.TerritoryViewModel
import java.util.Locale

@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    userId: String,
    authViewModel: AuthViewModel,
    territoryViewModel: TerritoryViewModel,
    onLogoutFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Query DB profile stats on compose
    LaunchedEffect(userId) {
        territoryViewModel.loadUserData(userId)
    }

    val activities by territoryViewModel.activities.collectAsState()
    val territories by territoryViewModel.territories.collectAsState()

    val totalRuns = activities.size
    val totalDistance = activities.sumOf { it.distance }
    val totalTerritories = territories.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Large profile initial glowing circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(RunPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.take(1).uppercase(Locale.getDefault()) else "A",
                    color = RunPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Name and Email
            Text(
                text = userName,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("profile_username_text")
            )

            Text(
                text = userEmail,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).testTag("profile_email_text")
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "EMPIRE CAREER SUMMARY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Start
            )

            // Dynamic Stats Grid in Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Line 1: Total Distance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Distance Traveled", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f km", totalDistance),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("profile_stat_total_distance")
                        )
                    }
                    Divider(color = RunSurfaceVariant, thickness = 1.dp)

                    // Line 2: Total Runs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Completed Runs", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            text = "$totalRuns",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("profile_stat_total_runs")
                        )
                    }
                    Divider(color = RunSurfaceVariant, thickness = 1.dp)

                    // Line 3: Captured territories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Territories Captured (Loops)", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            text = "$totalTerritories",
                            color = RunSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("profile_stat_total_territory")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Supabase connection details information card
            val isSupabase = com.example.data.SupabaseService.isConfigured()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSupabase) RunSecondary.copy(alpha = 0.12f) else GoldColor.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isSupabase) RunSecondary.copy(alpha = 0.4f) else GoldColor.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSupabase) "SUPABASE CLOUD ACTIVE" else "LOCAL OFFLINE STORAGE",
                        color = if (isSupabase) RunSecondary else GoldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSupabase) 
                            "Your runs, territories, and profile are fully encrypted and securely synced with your database server: \n${com.example.data.SupabaseService.url}"
                        else 
                            "Currently running offline on your local device. To enable real-time cloud sync and auth, add SUPABASE_URL and SUPABASE_KEY inside the Secrets panel of your AI Studio environment.",
                        color = if (isSupabase) TextPrimary.copy(alpha = 0.9f) else TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout interactive action
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogoutFinished()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Exit icon", tint = ErrorColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOGOUT CITIZEN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
