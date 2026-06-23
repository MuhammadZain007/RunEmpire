package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (userId: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            val user = authState as AuthState.Authenticated
            onRegisterSuccess(user.userId, user.name)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Join Run Empire",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Claim Your Turf Today",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Errors
            if (authState is AuthState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = ErrorColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { authViewModel.clearError() },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorColor)
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RunSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Full Name Field
                    Text(
                        text = "FULL NAME",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; authViewModel.clearError() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input")
                            .padding(bottom = 20.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User Icon", tint = TextSecondary) },
                        placeholder = { Text("enter your name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RunPrimary,
                            unfocusedBorderColor = RunSurfaceVariant,
                            focusedContainerColor = RunBackground,
                            unfocusedContainerColor = RunBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email Field
                    Text(
                        text = "EMAIL ADDRESS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; authViewModel.clearError() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                            .padding(bottom = 20.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = TextSecondary) },
                        placeholder = { Text("enter your email", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RunPrimary,
                            unfocusedBorderColor = RunSurfaceVariant,
                            focusedContainerColor = RunBackground,
                            unfocusedContainerColor = RunBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password Field
                    Text(
                        text = "PASSWORD (MIN 6 CHARACTERS)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; authViewModel.clearError() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                            .padding(bottom = 24.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = TextSecondary) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = "Toggle password visibility", tint = TextSecondary)
                            }
                        },
                        placeholder = { Text("••••••••", color = TextSecondary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RunPrimary,
                            unfocusedBorderColor = RunSurfaceVariant,
                            focusedContainerColor = RunBackground,
                            unfocusedContainerColor = RunBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Register Button
                    Button(
                        onClick = { authViewModel.register(name, email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_registration_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RunPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("BUILD MY EMPIRE", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already a citizen? ", color = TextSecondary, fontSize = 14.sp)
                TextButton(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.textButtonColors(contentColor = RunPrimary),
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("login_link")
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, fontSize = 14.sp)
                }
            }
        }
    }
}
