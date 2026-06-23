package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.RunEmpireApplication
import com.example.data.model.ProfileEntity
import com.example.data.repo.RunRepository
import com.example.data.SupabaseService
import com.example.data.SupabaseAuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val userId: String, val name: String, val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(private val repository: RunRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _forgotPasswordStatus = MutableStateFlow<String?>(null)
    val forgotPasswordStatus: StateFlow<String?> = _forgotPasswordStatus.asStateFlow()

    init {
        // Auto-login to active session if found locally
        val rootId = repository.getCurrentUserId()
        if (rootId != null) {
            viewModelScope.launch {
                val profile = repository.getProfileByIdSync(rootId)
                if (profile != null) {
                    _authState.value = AuthState.Authenticated(profile.id, profile.name, profile.email)
                }
            }
        }
    }

    fun register(name: String, email: String, psw: String) {
        if (name.isBlank() || email.isBlank() || psw.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }
        if (psw.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                if (SupabaseService.isConfigured()) {
                    // 1. Register with real Supabase Auth
                    when (val result = SupabaseService.signUp(name, email, psw)) {
                        is SupabaseAuthResult.Success -> {
                            // 2. Cache / insert profile details locally & remotely via repository
                            val newProfile = ProfileEntity(
                                id = result.userId,
                                name = result.name,
                                email = result.email,
                                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                            )
                            repository.insertProfile(newProfile)
                            repository.setSession(result.userId, result.email)
                            _authState.value = AuthState.Authenticated(result.userId, result.name, result.email)
                        }
                        is SupabaseAuthResult.Error -> {
                            _authState.value = AuthState.Error(result.message)
                        }
                    }
                } else {
                    // Fallback Offline Local Flow
                    val existing = repository.getProfileSync(email)
                    if (existing != null) {
                        _authState.value = AuthState.Error("An account with this email already exists")
                        return@launch
                    }

                    val newUserId = UUID.randomUUID().toString()
                    val newProfile = ProfileEntity(
                        id = newUserId,
                        name = name,
                        email = email,
                        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                    )
                    repository.insertProfile(newProfile)
                    repository.setSession(newUserId, email)
                    _authState.value = AuthState.Authenticated(newUserId, name, email)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration Error: ${e.message}")
            }
        }
    }

    fun login(email: String, psw: String) {
        if (email.isBlank() || psw.isBlank()) {
            _authState.value = AuthState.Error("Please fill in email and password")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                if (SupabaseService.isConfigured()) {
                    // 1. Sign in with real Supabase Auth
                    when (val result = SupabaseService.signIn(email, psw)) {
                        is SupabaseAuthResult.Success -> {
                            // 2. Cache / Sync profile locally
                            var localProfile = repository.getProfileByIdSync(result.userId)
                            if (localProfile == null) {
                                localProfile = ProfileEntity(
                                    id = result.userId,
                                    name = result.name,
                                    email = result.email,
                                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                                )
                                repository.insertProfile(localProfile)
                            }
                            repository.setSession(result.userId, result.email)
                            _authState.value = AuthState.Authenticated(result.userId, result.name, result.email)
                        }
                        is SupabaseAuthResult.Error -> {
                            _authState.value = AuthState.Error(result.message)
                        }
                    }
                } else {
                    // Fallback Offline Local Flow
                    val profile = repository.getProfileSync(email)
                    if (profile != null) {
                        repository.setSession(profile.id, profile.email)
                        _authState.value = AuthState.Authenticated(profile.id, profile.name, profile.email)
                    } else {
                        _authState.value = AuthState.Error("Incorrect email or password")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login Error: ${e.message}")
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordStatus.value = "Please enter a valid email address"
            return
        }
        _forgotPasswordStatus.value = "Sending recovery email..."
        viewModelScope.launch {
            try {
                if (SupabaseService.isConfigured()) {
                    val msg = SupabaseService.recoverPassword(email)
                    _forgotPasswordStatus.value = msg
                } else {
                    kotlinx.coroutines.delay(1000)
                    _forgotPasswordStatus.value = "Fallback Active: Password reset link mock-sent to $email."
                }
            } catch (e: Exception) {
                _forgotPasswordStatus.value = "Error sending email: ${e.message}"
            }
        }
    }

    fun clearForgotPasswordStatus() {
        _forgotPasswordStatus.value = null
    }

    fun logout() {
        repository.clearSession()
        _authState.value = AuthState.Idle
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}

class AuthViewModelFactory(private val repository: RunRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
