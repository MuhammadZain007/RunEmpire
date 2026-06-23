package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.TerritoryEntity
import com.example.data.model.ActivityEntity
import com.example.data.repo.RunRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerritoryViewModel(private val repository: RunRepository) : ViewModel() {

    private val _territories = MutableStateFlow<List<TerritoryEntity>>(emptyList())
    val territories: StateFlow<List<TerritoryEntity>> = _territories.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityEntity>>(emptyList())
    val activities: StateFlow<List<ActivityEntity>> = _activities.asStateFlow()

    private var activeJobTerritory: kotlinx.coroutines.Job? = null
    private var activeJobActivity: kotlinx.coroutines.Job? = null

    fun loadUserData(userId: String) {
        activeJobTerritory?.cancel()
        activeJobActivity?.cancel()

        activeJobTerritory = viewModelScope.launch {
            repository.getAllTerritories(userId).collect { list ->
                _territories.value = list
            }
        }

        activeJobActivity = viewModelScope.launch {
            repository.getAllActivities(userId).collect { list ->
                _activities.value = list
            }
        }
    }
}

class TerritoryViewModelFactory(private val repository: RunRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TerritoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TerritoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
