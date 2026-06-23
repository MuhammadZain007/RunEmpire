package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Instantiate our ViewModels with their custom Factories
                val app = RunEmpireApplication.instance
                val repository = app.repository

                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(repository)
                )
                val runViewModel: RunViewModel = viewModel(
                    factory = RunViewModelFactory(applicationContext, repository)
                )
                val territoryViewModel: TerritoryViewModel = viewModel(
                    factory = TerritoryViewModelFactory(repository)
                )

                // Navigation Stack Router
                val screenStack = remember { mutableStateListOf("splash") }
                val currentScreen = screenStack.lastOrNull() ?: "splash"
                
                // Active session details
                val authState by authViewModel.authState.collectAsState()
                var userName by remember { mutableStateOf("Runner") }
                var userEmail by remember { mutableStateOf("") }
                var userId by remember { mutableStateOf("") }

                // Selected activity ID for history details parameter passing
                var selectedActivityId by remember { mutableStateOf("") }

                // Synchronize session details when authenticated
                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        val session = authState as AuthState.Authenticated
                        userName = session.name
                        userEmail = session.email
                        userId = session.userId
                    }
                }

                // Helper navigation handlers
                val navigateTo: (String) -> Unit = { screen ->
                    screenStack.add(screen)
                }

                val navigateBack: () -> Unit = {
                    if (screenStack.size > 1) {
                        screenStack.removeAt(screenStack.lastIndex)
                    }
                }

                val navigateToClear: (String) -> Unit = { screen ->
                    screenStack.clear()
                    screenStack.add(screen)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BoxModifier(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "splash" -> SplashScreen(
                                isSessionActive = authState is AuthState.Authenticated,
                                onNavigateToLogin = { navigateToClear("login") },
                                onNavigateToHome = { navigateToClear("main") }
                            )

                            "login" -> LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToRegister = { navigateTo("register") },
                                onNavigateToForgotPassword = { navigateTo("forgot_password") },
                                onLoginSuccess = { uid, name ->
                                    userId = uid
                                    userName = name
                                    navigateToClear("main")
                                }
                            )

                            "register" -> RegisterScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = { navigateBack() },
                                onRegisterSuccess = { uid, name ->
                                    userId = uid
                                    userName = name
                                    navigateToClear("main")
                                }
                            )

                            "forgot_password" -> ForgotPasswordScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { navigateBack() }
                            )

                            "main" -> MainContainerScreen(
                                userName = userName,
                                userEmail = userEmail,
                                userId = userId,
                                authViewModel = authViewModel,
                                territoryViewModel = territoryViewModel,
                                onNavigateToLiveRun = { navigateTo("live_run") },
                                onSelectActivity = { actId ->
                                    selectedActivityId = actId
                                    navigateTo("detail")
                                },
                                onLogoutFinished = { navigateToClear("login") }
                            )

                            "live_run" -> LiveRunScreen(
                                runViewModel = runViewModel,
                                onNavigateBack = { navigateBack() },
                                onRunFinished = { navigateTo("summary") }
                            )

                            "summary" -> RunSummaryScreen(
                                runViewModel = runViewModel,
                                onNavigateHome = {
                                    // Pop back stack cleanly to dashboard
                                    navigateToClear("main")
                                }
                            )

                            "detail" -> ActivityDetailScreen(
                                activityId = selectedActivityId,
                                repository = repository,
                                onNavigateBack = { navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Inline wrapper helper to set layout scopes
@Composable
fun BoxModifier(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
