package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.TerritoryViewModel

@Composable
fun MainContainerScreen(
    userName: String,
    userEmail: String,
    userId: String,
    authViewModel: AuthViewModel,
    territoryViewModel: TerritoryViewModel,
    onNavigateToLiveRun: () -> Unit,
    onSelectActivity: (String) -> Unit,
    onLogoutFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = RunSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home tab", tint = if (selectedTab == 0) RunPrimary else TextSecondary) },
                    label = { Text("Home", color = if (selectedTab == 0) RunPrimary else TextSecondary) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RunPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 1: Map
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "Territory map tab", tint = if (selectedTab == 1) RunSecondary else TextSecondary) },
                    label = { Text("Territory", color = if (selectedTab == 1) RunSecondary else TextSecondary) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RunSecondary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_territory")
                )

                // Tab 2: History
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Activity history tab", tint = if (selectedTab == 2) RunPrimary else TextSecondary) },
                    label = { Text("History", color = if (selectedTab == 2) RunPrimary else TextSecondary) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RunPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Tab 3: Profile
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "User profile tab", tint = if (selectedTab == 3) RunPrimary else TextSecondary) },
                    label = { Text("Profile", color = if (selectedTab == 3) RunPrimary else TextSecondary) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RunPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        },
        containerColor = RunBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    userName = userName,
                    userId = userId,
                    territoryViewModel = territoryViewModel,
                    onNavigateToLiveRun = onNavigateToLiveRun
                )
                1 -> TerritoryMapScreen(
                    userId = userId,
                    territoryViewModel = territoryViewModel
                )
                2 -> ActivityHistoryScreen(
                    userId = userId,
                    territoryViewModel = territoryViewModel,
                    onSelectActivity = onSelectActivity
                )
                3 -> ProfileScreen(
                    userName = userName,
                    userEmail = userEmail,
                    userId = userId,
                    authViewModel = authViewModel,
                    territoryViewModel = territoryViewModel,
                    onLogoutFinished = onLogoutFinished
                )
            }
        }
    }
}
